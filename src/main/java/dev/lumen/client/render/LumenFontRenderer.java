package dev.lumen.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.lumen.client.font.FontManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * STB TrueType glyph-atlas renderer. Each font is baked once at high
 * resolution (64px glyphs into a 1024x1024 alpha atlas) and drawn scaled
 * down to the on-screen size, so text stays crisp at any Minecraft GUI
 * scale instead of being upscaled from a tiny bitmap. Glyph metrics are
 * cached in primitive arrays and drawing goes through the vanilla GUI
 * textured pipeline - zero allocation per frame.
 */
public class LumenFontRenderer {
    public static final int ATLAS_SIZE = 1024;
    public static final int FIRST_CHAR = 32;
    public static final int CHAR_COUNT = 224;
    public static final int BAKE_PIXEL_SIZE = 64;
    public static final float DRAW_SIZE = 13.0f;
    private static final float DRAW_SCALE = DRAW_SIZE / BAKE_PIXEL_SIZE;

    private final String id;
    private final ByteBuffer fontData;

    private Identifier atlasId;
    private STBTTBakedChar.Buffer bakedChars;
    private final float[] advances = new float[CHAR_COUNT];
    private boolean initialized;
    private boolean failed;

    public LumenFontRenderer(String id, ByteBuffer fontData) {
        this.id = id;
        this.fontData = fontData;
    }

    public String getId() {
        return id;
    }

    public boolean isAvailable() {
        return initialized && !failed;
    }

    /** Forces atlas baking immediately (e.g. when the font is selected). */
    public void warmUp() {
        initialize();
    }

    private synchronized void initialize() {
        if (initialized || failed) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getTextureManager() == null) return;

        STBTTBakedChar.Buffer chars = STBTTBakedChar.malloc(CHAR_COUNT);
        ByteBuffer bitmap = MemoryUtil.memAlloc(ATLAS_SIZE * ATLAS_SIZE);
        int result = STBTruetype.stbtt_BakeFontBitmap(fontData, BAKE_PIXEL_SIZE, bitmap, ATLAS_SIZE, ATLAS_SIZE, FIRST_CHAR, chars);
        if (result <= 0) {
            MemoryUtil.memFree(bitmap);
            chars.free();
            failed = true;
            dev.lumen.client.LumenClient.LOGGER.warn("Font bake failed for '{}' (atlas too small); falling back to vanilla font", id);
            return;
        }

        NativeImage image = new NativeImage(ATLAS_SIZE, ATLAS_SIZE, false);
        for (int y = 0; y < ATLAS_SIZE; y++) {
            for (int x = 0; x < ATLAS_SIZE; x++) {
                int alpha = bitmap.get(y * ATLAS_SIZE + x) & 0xFF;
                if (alpha != 0) {
                    image.setPixelABGR(x, y, (alpha << 24) | 0x00FFFFFF);
                }
            }
        }
        MemoryUtil.memFree(bitmap);

        this.atlasId = Identifier.fromNamespaceAndPath("lumen", "font_atlas_" + id);
        DynamicTexture texture = new DynamicTexture(() -> "Lumen Font Atlas [" + id + "]", image);
        minecraft.getTextureManager().register(atlasId, texture);

        for (int i = 0; i < CHAR_COUNT; i++) {
            advances[i] = chars.get(i).xadvance();
        }
        this.bakedChars = chars;
        this.initialized = true;
    }

    public float getWidth(CharSequence text) {
        if (!isAvailable()) initialize();
        if (!isAvailable()) return text.length() * 6.0f;
        float width = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            int index = text.charAt(i) - FIRST_CHAR;
            width += (index >= 0 && index < CHAR_COUNT) ? advances[index] : BAKE_PIXEL_SIZE * 0.3f;
        }
        return width * DRAW_SCALE;
    }

    public float getCharHeight() {
        return DRAW_SIZE * 0.9f;
    }

    public void drawString(GuiGraphics graphics, CharSequence text, float x, float y, int argb, boolean shadow) {
        if (!isAvailable()) initialize();
        if (!isAvailable()) return;
        if (shadow) {
            drawScaled(graphics, text, x + 1.0f, y + 1.0f, (argb & 0xFF000000), 1.0f);
        }
        drawScaled(graphics, text, x, y, argb, 1.0f);
    }

    private void drawInternal(GuiGraphics graphics, CharSequence text, int argb) {
        float cursor = 0.0f;
        float baseline = BAKE_PIXEL_SIZE * 0.78f;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int index = c - FIRST_CHAR;
            if (c == '\u00A7' && i + 1 < text.length()) {
                i++;
                continue;
            }
            if (index < 0 || index >= CHAR_COUNT) {
                cursor += BAKE_PIXEL_SIZE * 0.3f;
                continue;
            }
            STBTTBakedChar glyph = bakedChars.get(index);
            int gx0 = glyph.x0();
            int gy0 = glyph.y0();
            int gx1 = glyph.x1();
            int gy1 = glyph.y1();
            int w = gx1 - gx0;
            int h = gy1 - gy0;
            if (w > 0 && h > 0) {
                graphics.blit(
                        net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                        atlasId,
                        Math.round(cursor + glyph.xoff()),
                        Math.round(baseline + glyph.yoff()),
                        gx0, gy0,
                        w, h,
                        ATLAS_SIZE, ATLAS_SIZE,
                        argb
                );
            }
            cursor += advances[index];
        }
    }

    public void drawScaledString(GuiGraphics graphics, CharSequence text, float x, float y, int argb, float scale, boolean shadow) {
        if (!isAvailable()) initialize();
        if (!isAvailable()) return;
        if (shadow) {
            drawScaled(graphics, text, x + 1.0f, y + 1.0f, (argb & 0xFF000000), scale);
        }
        drawScaled(graphics, text, x, y, argb, scale);
    }

    private void drawScaled(GuiGraphics graphics, CharSequence text, float x, float y, int argb, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        float s = DRAW_SCALE * scale;
        graphics.pose().scale(s, s);
        drawInternal(graphics, text, argb);
        graphics.pose().popMatrix();
    }

    public float getScaledWidth(CharSequence text, float scale) {
        return getWidth(text) * scale;
    }
}
