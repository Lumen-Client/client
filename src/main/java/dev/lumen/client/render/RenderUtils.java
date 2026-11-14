package dev.lumen.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.lumen.client.font.FontManager;
import dev.lumen.client.theme.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Anti-aliased rounded rectangles, gradients, drop shadows, color wheel and
 * gradient text - all backed by cached dynamic textures so repeated frames
 * never allocate or regenerate pixels.
 */
public final class RenderUtils {
    private static final int SHADOW_TEX = 64;
    private static final int WHEEL_TEX = 128;
    private static final int CORNER_TEX = 64;

    private static final Map<Integer, Identifier> roundedCache = new LinkedHashMap<>(32, 0.75f, false);
    private static final Map<Long, Identifier> gradientCache = new LinkedHashMap<>(64, 0.75f, false);
    private static Identifier shadowTexture;
    private static Identifier wheelTexture;
    private static Identifier barTexture;

    private RenderUtils() {}

    public static void fill(GuiGraphics graphics, float x, float y, float w, float h, int argb) {
        if ((argb >>> 24) == 0) return;
        graphics.fill((int) x, (int) y, (int) (x + w), (int) (y + h), argb);
    }

    public static void verticalGradient(GuiGraphics graphics, float x, float y, float w, float h, int from, int to) {
        graphics.fillGradient((int) x, (int) y, (int) (x + w), (int) (y + h), from, to);
    }

    public static void border(GuiGraphics graphics, float x, float y, float w, float h, float thickness, int argb) {
        fill(graphics, x, y, w, thickness, argb);
        fill(graphics, x, y + h - thickness, w, thickness, argb);
        fill(graphics, x, y + thickness, thickness, h - thickness * 2.0f, argb);
        fill(graphics, x + w - thickness, y + thickness, thickness, h - thickness * 2.0f, argb);
    }

    private static Identifier getRoundedTexture(int radius) {
        int r = Math.max(1, Math.min(24, radius));
        Identifier cached = roundedCache.get(r);
        if (cached != null) return cached;
        cached = generateRoundedTexture(r);
        roundedCache.put(r, cached);
        evictIfNeeded(roundedCache, 32);
        return cached;
    }

    private static Identifier generateRoundedTexture(int radius) {
        NativeImage image = new NativeImage(CORNER_TEX, CORNER_TEX, false);
        float center = (CORNER_TEX - 1) / 2.0f;
        float inner = center - radius;
        for (int y = 0; y < CORNER_TEX; y++) {
            for (int x = 0; x < CORNER_TEX; x++) {
                float coverage = roundedCoverage(x, y, center, center, inner, inner, radius);
                int alpha = (int) (coverage * 255.0f);
                image.setPixelABGR(x, y, (alpha << 24) | 0x00FFFFFF);
            }
        }
        Identifier id = Identifier.fromNamespaceAndPath("lumen", "rounded_" + radius);
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "Lumen Rounded " + radius, image));
        return id;
    }

    /** Signed-distance coverage of a rounded rectangle, 0..1, anti-aliased. */
    private static float roundedCoverage(float x, float y, float centerX, float centerY, float innerX, float innerY, float radius) {
        float qx = Math.abs(x - centerX) - innerX;
        float qy = Math.abs(y - centerY) - innerY;
        float outsideX = Math.max(qx, 0.0f);
        float outsideY = Math.max(qy, 0.0f);
        float outsideDist = (float) Math.sqrt(outsideX * outsideX + outsideY * outsideY);
        float insideDist = Math.min(Math.max(qx, qy), 0.0f);
        float dist = outsideDist + insideDist - radius;
        return Math.max(0.0f, Math.min(1.0f, 0.5f - dist));
    }

    public static void roundedRect(GuiGraphics graphics, float x, float y, float w, float h, float radius, int argb) {
        if (w <= 0 || h <= 0 || (argb >>> 24) == 0) return;
        int r = (int) Math.min(radius, Math.min(w, h) / 2.0f);
        if (r < 1) {
            fill(graphics, x, y, w, h, argb);
            return;
        }
        Identifier tex = getRoundedTexture(r);
        int corner = Math.min(r + 6, CORNER_TEX / 2);
        if (corner * 2 > w) corner = (int) (w / 2.0f);
        if (corner * 2 > h) corner = (int) (h / 2.0f);
        if (corner < 1) {
            fill(graphics, x, y, w, h, argb);
            return;
        }
        float midW = w - corner * 2.0f;
        float midH = h - corner * 2.0f;

        if (midW > 0 && midH > 0) {
            fill(graphics, x + corner, y + corner, midW, midH, argb);
        }
        if (midW > 0) {
            fill(graphics, x + corner, y, midW, corner, argb);
            fill(graphics, x + corner, y + h - corner, midW, corner, argb);
        }
        if (midH > 0) {
            fill(graphics, x, y + corner, corner, midH, argb);
            fill(graphics, x + w - corner, y + corner, corner, midH, argb);
        }
        drawCorner(graphics, tex, x, y, corner, 0, 0, argb);
        drawCorner(graphics, tex, x + w - corner, y, corner, CORNER_TEX - corner, 0, argb);
        drawCorner(graphics, tex, x, y + h - corner, corner, 0, CORNER_TEX - corner, argb);
        drawCorner(graphics, tex, x + w - corner, y + h - corner, corner, CORNER_TEX - corner, CORNER_TEX - corner, argb);
    }

    private static void drawCorner(GuiGraphics graphics, Identifier tex, float x, float y, int size, int u, int v, int argb) {
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex, (int) x, (int) y, u, v, size, size, CORNER_TEX, CORNER_TEX, argb);
    }

    public static void roundedGradient(GuiGraphics graphics, float x, float y, float w, float h, float radius, int from, int to, boolean vertical) {
        if (w <= 0 || h <= 0) return;
        long key = (((long) (int) w) << 40) ^ (((long) (int) h) << 24) ^ (((long) (int) radius) << 12)
                ^ (((long) (from & 0xFFFFFF)) << 46) ^ (((long) (to & 0xFFFFFF)) << 22)
                ^ ((long) (from >>> 24) << 8) ^ ((long) (to >>> 24) << 10) ^ (vertical ? 0x2L : 0L);
        Identifier tex = gradientCache.get(key);
        if (tex == null) {
            tex = generateGradientTexture((int) w, (int) h, (int) radius, from, to, vertical);
            gradientCache.put(key, tex);
            evictIfNeeded(gradientCache, 96);
        }
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex, (int) x, (int) y, 0, 0, (int) w, (int) h, (int) w, (int) h, 0xFFFFFFFF);
    }

    private static Identifier generateGradientTexture(int w, int h, int radius, int from, int to, boolean vertical) {
        w = Math.max(1, w);
        h = Math.max(1, h);
        NativeImage image = new NativeImage(w, h, false);
        float centerX = (w - 1) / 2.0f;
        float centerY = (h - 1) / 2.0f;
        float inner = Math.min(w, h) / 2.0f - radius;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float t = vertical ? (h > 1 ? y / (float) (h - 1) : 0.0f) : (w > 1 ? x / (float) (w - 1) : 0.0f);
                int color = ColorUtils.lerp(from, to, t);
                int alpha = ColorUtils.alphaOf(color);
                float innerX = centerX - radius;
                float innerY = centerY - radius;
                float coverage = roundedCoverage(x, y, centerX, centerY, innerX, innerY, radius);
                alpha = (int) (alpha * coverage);
                image.setPixelABGR(x, y, (alpha << 24) | (color & 0xFFFFFF));
            }
        }
        Identifier id = Identifier.fromNamespaceAndPath("lumen", "grad_" + GRAD_COUNTER.incrementAndGet());
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "Lumen Gradient", image));
        return id;
    }

    private static final java.util.concurrent.atomic.AtomicInteger GRAD_COUNTER = new java.util.concurrent.atomic.AtomicInteger();

    public static void dropShadow(GuiGraphics graphics, float x, float y, float w, float h) {
        int soft = 0x38000000;
        roundedRect(graphics, x - 1.5f, y + 2.0f, w + 3.0f, h + 3.0f, 8.0f, soft);
        roundedRect(graphics, x - 3.0f, y + 5.0f, w + 6.0f, h + 4.0f, 10.0f, 0x18000000);
    }

    private static Identifier generateRadialTexture() {
        NativeImage image = new NativeImage(SHADOW_TEX, SHADOW_TEX, false);
        float center = (SHADOW_TEX - 1) / 2.0f;
        for (int y = 0; y < SHADOW_TEX; y++) {
            for (int x = 0; x < SHADOW_TEX; x++) {
                float dx = (x - center) / center;
                float dy = (y - center) / center;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float alpha = Math.max(0.0f, 1.0f - dist);
                image.setPixelABGR(x, y, ((int) (alpha * 255.0f) << 24) | 0x00FFFFFF);
            }
        }
        Identifier id = Identifier.fromNamespaceAndPath("lumen", "shadow_blob");
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "Lumen Shadow", image));
        return id;
    }

    public static final Identifier LOGO_TEXTURE = Identifier.fromNamespaceAndPath("lumen", "icon.png");

    /**
     * Draws a full registered texture (e.g. the client logo) scaled to w x h.
     */
    public static void drawTexture(GuiGraphics graphics, Identifier texture, float x, float y, float w, float h, int argb) {
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture,
                (int) x, (int) y, 0.0f, 0.0f, (int) w, (int) h, (int) w, (int) h, argb);
    }

    public static void colorWheel(GuiGraphics graphics, float x, float y, float size) {
        if (wheelTexture == null) {
            wheelTexture = generateWheelTexture();
        }
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, wheelTexture,
                (int) x, (int) y, 0, 0, (int) size, (int) size, WHEEL_TEX, WHEEL_TEX, 0xFFFFFFFF);
    }

    private static Identifier generateWheelTexture() {
        NativeImage image = new NativeImage(WHEEL_TEX, WHEEL_TEX, false);
        float center = (WHEEL_TEX - 1) / 2.0f;
        for (int y = 0; y < WHEEL_TEX; y++) {
            for (int x = 0; x < WHEEL_TEX; x++) {
                float dx = (x - center) / center;
                float dy = (y - center) / center;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > 1.0f) {
                    image.setPixelABGR(x, y, 0);
                    continue;
                }
                float hue = (float) (Math.atan2(dy, dx) / (Math.PI * 2.0));
                if (hue < 0.0f) hue += 1.0f;
                float sat = Math.min(1.0f, dist);
                int rgb = java.awt.Color.HSBtoRGB(hue, sat, 1.0f) & 0xFFFFFF;
                image.setPixelABGR(x, y, (dist < 0.985f ? 0xFF000000 : 0xC0000000) | rgb);
            }
        }
        Identifier id = Identifier.fromNamespaceAndPath("lumen", "color_wheel");
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "Lumen Color Wheel", image));
        return id;
    }

    public static void brightnessBar(GuiGraphics graphics, float x, float y, float w, float h) {
        if (barTexture == null) {
            barTexture = generateBarTexture();
        }
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, barTexture,
                (int) x, (int) y, 0, 0, (int) w, (int) h, 256, 16, 0xFFFFFFFF);
    }

    private static Identifier generateBarTexture() {
        NativeImage image = new NativeImage(256, 16, false);
        for (int x = 0; x < 256; x++) {
            int color = java.awt.Color.HSBtoRGB(0.0f, 0.0f, x / 255.0f) & 0xFFFFFF;
            for (int y = 0; y < 16; y++) {
                image.setPixelABGR(x, y, 0xFF000000 | color);
            }
        }
        Identifier id = Identifier.fromNamespaceAndPath("lumen", "brightness_bar");
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "Lumen Brightness Bar", image));
        return id;
    }

    public static void drawGradientText(GuiGraphics graphics, Font font, String text, float x, float y, int from, int to, boolean shadow, float phase) {
        int length = text.length();
        int charX = (int) x;
        for (int i = 0; i < length; i++) {
            String ch = String.valueOf(text.charAt(i));
            float t = (i / (float) Math.max(1, length - 1)) * 0.6f + phase;
            t -= Math.floor(t);
            int color = ColorUtils.lerp(from, to, t);
            if (shadow) {
                graphics.drawString(font, ch, charX + 1, (int) y + 1, ColorUtils.withAlpha(0xFF000000, ColorUtils.alphaOf(color)), false);
            }
            graphics.drawString(font, ch, charX, (int) y, color, false);
            charX += font.width(ch);
        }
    }

    public static void drawGradientTextCustom(GuiGraphics graphics, String text, float x, float y, int from, int to, boolean shadow, float phase) {
        LumenFontRenderer renderer = FontManager.get().getActiveRenderer();
        if (renderer == null) {
            drawGradientText(graphics, Minecraft.getInstance().font, text, x, y, from, to, shadow, phase);
            return;
        }
        int length = text.length();
        float charX = x;
        for (int i = 0; i < length; i++) {
            float t = (i / (float) Math.max(1, length - 1)) * 0.6f + phase;
            t -= Math.floor(t);
            int color = ColorUtils.lerp(from, to, t);
            String ch = String.valueOf(text.charAt(i));
            renderer.drawString(graphics, ch, charX, y, color, shadow);
            charX += renderer.getWidth(ch);
        }
    }

    private static void evictIfNeeded(Map<?, ?> cache, int max) {
        Iterator<?> it = cache.keySet().iterator();
        while (cache.size() > max && it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    public static void clearCaches() {
        roundedCache.clear();
        gradientCache.clear();
    }
}
