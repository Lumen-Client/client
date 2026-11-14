package dev.lumen.client.modules.hud;

import dev.lumen.client.core.HudModule;
import dev.lumen.client.render.RenderUtils;
import dev.lumen.client.render.TextRenderer;
import dev.lumen.client.theme.ColorUtils;
import dev.lumen.client.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public class CompassModule extends HudModule {
    private static final String[] TICK_LABELS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    public CompassModule() {
        super("Compass", "Scrolling cardinal direction bar", 160.0f, 6.0f);
    }

    @Override
    public float getHudWidth() {
        return 160.0f;
    }

    @Override
    public float getHudHeight() {
        return TextRenderer.height() + 10.0f;
    }

    @Override
    public void renderHud(GuiGraphics graphics, float delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ThemeManager theme = ThemeManager.get();
        float yaw = Mth.wrapDegrees(mc.player.getYRot());
        float width = getHudWidth();
        float centerX = width / 2.0f;
        float pixelsPerDegree = width / 180.0f;

        RenderUtils.verticalGradient(graphics, 0.0f, 0.0f, width, getHudHeight(),
                ColorUtils.withAlpha(theme.getBackground(), 0), ColorUtils.withAlpha(theme.getBackground(), 160));

        for (int deg = -180; deg <= 180; deg += 45) {
            float diff = Mth.wrapDegrees(deg - yaw);
            if (Math.abs(diff) > 90.0f) continue;
            float px = centerX + diff * pixelsPerDegree;
            int labelIndex = ((deg + 180) / 45) % 8;
            String label = TICK_LABELS[labelIndex < 0 ? labelIndex + 8 : labelIndex];
            float fade = 1.0f - Math.abs(diff) / 90.0f;
            int color = ColorUtils.withAlpha(theme.getText(), (int) (140 + 115 * fade));
            TextRenderer.draw(graphics, label, px - TextRenderer.width(label) / 2.0f, 4.0f, color, true);
        }

        RenderUtils.fill(graphics, centerX - 1.0f, 2.0f, 2.0f, getHudHeight() - 4.0f, theme.gradientColor(0.5f));
    }
}
