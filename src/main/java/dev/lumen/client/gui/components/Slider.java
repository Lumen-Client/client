package dev.lumen.client.gui.components;

import dev.lumen.client.core.setting.NumberSetting;
import dev.lumen.client.render.RenderUtils;
import dev.lumen.client.render.TextRenderer;
import dev.lumen.client.theme.ColorUtils;
import dev.lumen.client.theme.ThemeManager;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Smooth slider with animated fill, hover glow and live numeric value.
 */
public class Slider {
    private boolean dragging;
    private boolean hovered;

    public boolean isDragging() {
        return dragging;
    }

    public boolean mouseDown(double mouseX, double mouseY, NumberSetting setting, float x, float y, float w, float h) {
        if (mouseX >= x && mouseX <= x + w && mouseY >= y - 2 && mouseY <= y + h + 2) {
            dragging = true;
            apply(mouseX, setting, x, w);
            return true;
        }
        return false;
    }

    public void drag(double mouseX, NumberSetting setting, float x, float w) {
        if (dragging) {
            apply(mouseX, setting, x, w);
        }
    }

    public boolean release() {
        boolean was = dragging;
        dragging = false;
        return was;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    private void apply(double mouseX, NumberSetting setting, float x, float w) {
        float percent = (float) ((mouseX - x) / w);
        percent = Math.max(0.0f, Math.min(1.0f, percent));
        setting.set(setting.getMin() + percent * (setting.getMax() - setting.getMin()));
    }

    public void render(GuiGraphics graphics, float x, float y, float w, float h, NumberSetting setting, float alpha) {
        ThemeManager theme = ThemeManager.get();
        float percent = setting.getPercent();
        int track = ColorUtils.withAlpha(0xFF2A2A33, (int) (255 * alpha));
        RenderUtils.roundedRect(graphics, x, y, w, h, h / 2.0f, track);

        float fillW = Math.max(h, w * percent);
        int from = theme.gradientColor(0.0f, alpha);
        int to = theme.gradientColor(1.0f, alpha);
        RenderUtils.roundedGradient(graphics, x, y, fillW, h, h / 2.0f, from, to, false);

        float glow = hovered || dragging ? 1.5f : 0.0f;
        float thumbR = h / 2.0f + glow;
        int thumb = ColorUtils.withAlpha(0xFFFFFFFF, (int) (255 * alpha));
        RenderUtils.roundedRect(graphics, x + fillW - thumbR, y + h / 2.0f - thumbR, thumbR * 2.0f, thumbR * 2.0f, thumbR, thumb);

        String label = setting.getName() + ": " + setting.getDisplayValue();
        TextRenderer.draw(graphics, label, x, y - 11.0f, ColorUtils.withAlpha(theme.getText(), (int) (230 * alpha)), true);
    }
}
