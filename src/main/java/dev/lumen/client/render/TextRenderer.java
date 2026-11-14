package dev.lumen.client.render;

import dev.lumen.client.font.FontManager;
import dev.lumen.client.render.LumenFontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Facade that draws with the active Lumen font, falling back to the vanilla
 * font when selected or unavailable.
 */
public final class TextRenderer {
    private TextRenderer() {}

    public static Font vanilla() {
        return Minecraft.getInstance().font;
    }

    public static float width(String text) {
        LumenFontRenderer renderer = FontManager.get().getActiveRenderer();
        float scale = FontManager.get().getFontScale();
        if (renderer != null) {
            return renderer.getWidth(text) * scale;
        }
        return vanilla().width(text) * scale;
    }

    public static float height() {
        LumenFontRenderer renderer = FontManager.get().getActiveRenderer();
        if (renderer != null) {
            return renderer.getCharHeight();
        }
        return vanilla().lineHeight;
    }

    public static void draw(GuiGraphics graphics, String text, float x, float y, int argb, boolean shadow) {
        float fontScale = FontManager.get().getFontScale();
        if (fontScale != 1.0f) {
            drawScaled(graphics, text, x, y, argb, fontScale, shadow);
            return;
        }
        LumenFontRenderer renderer = FontManager.get().getActiveRenderer();
        if (renderer != null) {
            FontManager.get().ensureActiveRenderer();
            renderer = FontManager.get().getActiveRenderer();
        }
        if (renderer != null) {
            renderer.drawString(graphics, text, x, y, argb, shadow && FontManager.get().hasShadow());
        } else {
            graphics.drawString(vanilla(), text, (int) x, (int) y, argb, shadow);
        }
    }

    public static void drawScaled(GuiGraphics graphics, String text, float x, float y, int argb, float scale, boolean shadow) {
        LumenFontRenderer renderer = FontManager.get().getActiveRenderer();
        if (renderer != null) {
            FontManager.get().ensureActiveRenderer();
            renderer = FontManager.get().getActiveRenderer();
        }
        if (renderer != null) {
            renderer.drawScaledString(graphics, text, x, y, argb, scale, shadow && FontManager.get().hasShadow());
        } else {
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().scale(scale, scale);
            graphics.drawString(vanilla(), text, 0, 0, argb, shadow);
            graphics.pose().popMatrix();
        }
    }
}
