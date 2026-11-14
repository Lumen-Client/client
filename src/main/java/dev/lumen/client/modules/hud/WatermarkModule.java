package dev.lumen.client.modules.hud;

import dev.lumen.client.Lumen;
import dev.lumen.client.core.HudModule;
import dev.lumen.client.render.RenderUtils;
import dev.lumen.client.render.TextRenderer;
import dev.lumen.client.theme.ColorUtils;
import dev.lumen.client.theme.ThemeManager;
import net.minecraft.client.gui.GuiGraphics;

public class WatermarkModule extends HudModule {
    private float shimmerPhase;

    public WatermarkModule() {
        super("Watermark", "Animated Lumen Client watermark");
    }

    @Override
    public void onRenderTick() {
        shimmerPhase += 0.008f * ThemeManager.get().getGradientSpeed();
        if (shimmerPhase > 1.0f) shimmerPhase -= 1.0f;
    }

    @Override
    public float getHudWidth() {
        return TextRenderer.width(Lumen.WATERMARK) + 10.0f;
    }

    @Override
    public float getHudHeight() {
        return TextRenderer.height() + 8.0f;
    }

    @Override
    public void renderHud(GuiGraphics graphics, float delta) {
        ThemeManager theme = ThemeManager.get();
        int from = theme.getPrimary();
        int to = theme.getSecondary();
        float textY = 4.0f;
        float textX = 5.0f;
        if (theme.getGradientMode() == ThemeManager.GradientMode.SOLID) {
            TextRenderer.draw(graphics, Lumen.WATERMARK, textX, textY, from, true);
        } else {
            RenderUtils.drawGradientTextCustom(graphics, Lumen.WATERMARK, textX, textY, from, to, true, shimmerPhase);
        }
    }
}
