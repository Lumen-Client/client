package dev.lumen.client.gui.components;

import dev.lumen.client.anim.Animation;
import dev.lumen.client.anim.Easing;
import dev.lumen.client.render.RenderUtils;
import dev.lumen.client.theme.ColorUtils;
import dev.lumen.client.theme.ThemeManager;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Animated toggle switch. One instance per setting, reused every frame -
 * no per-frame allocation.
 */
public class ToggleSwitch {
    private final Animation anim = new Animation(0.16f, Easing::easeOutCubic);

    public void render(GuiGraphics graphics, float x, float y, float w, float h, boolean on, float alpha) {
        anim.setTarget(on);
        anim.update();
        float t = anim.getValue();

        ThemeManager theme = ThemeManager.get();
        int trackOff = ColorUtils.withAlpha(0xFF3A3A44, (int) (255 * alpha));
        int track = ColorUtils.lerp(trackOff, ColorUtils.scaleAlpha(theme.getPrimary(), alpha), t);
        float radius = h / 2.0f;
        RenderUtils.roundedRect(graphics, x, y, w, h, radius, track);

        float thumbSize = h - 4.0f;
        float thumbX = x + 2.0f + (w - thumbSize - 4.0f) * t;
        int thumbColor = ColorUtils.withAlpha(0xFFFFFFFF, (int) (255 * alpha));
        RenderUtils.roundedRect(graphics, thumbX, y + 2.0f, thumbSize, thumbSize, radius - 2.0f, thumbColor);
    }
}
