package dev.lumen.client.modules.hud;

import dev.lumen.client.anim.Animation;
import dev.lumen.client.anim.AnimationUtils;
import dev.lumen.client.anim.Easing;
import dev.lumen.client.core.HudModule;
import dev.lumen.client.render.RenderUtils;
import dev.lumen.client.render.TextRenderer;
import dev.lumen.client.theme.ColorUtils;
import dev.lumen.client.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class TargetHudModule extends HudModule {
    private static final float HEART_W = 8.0f;
    private static final float HEART_H = 7.0f;
    private static final int HEART_COUNT = 10;

    private final Animation showAnim = new Animation(0.2f, Easing::easeOutCubic);
    private final Animation healthAnim = new Animation(0.25f, Easing::easeOutCubic);
    private String targetName = "";
    private float targetHealthPercent;

    public TargetHudModule() {
        super("Target HUD", "Health bar and hearts of the entity you are looking at", 170.0f, 90.0f);
    }

    @Override
    public void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        Entity target = mc.crosshairPickEntity;
        boolean valid = target instanceof LivingEntity living && living.isAlive() && living != mc.player;
        showAnim.setTarget(valid);
        if (valid) {
            LivingEntity living = (LivingEntity) target;
            targetName = living.getName().getString();
            targetHealthPercent = living.getHealth() / Math.max(1.0f, living.getMaxHealth());
            healthAnim.setTarget(true);
        }
    }

    @Override
    public float getHudWidth() {
        return 130.0f;
    }

    @Override
    public float getHudHeight() {
        return 46.0f;
    }

    @Override
    public boolean isContentEmpty() {
        return showAnim.getValue() < 0.02f;
    }

    @Override
    public void renderHud(GuiGraphics graphics, float delta) {
        showAnim.update();
        float alpha = showAnim.getValue();
        if (alpha < 0.02f) return;
        ThemeManager theme = ThemeManager.get();

        RenderUtils.roundedRect(graphics, 0.0f, 0.0f, 130.0f, 46.0f, 5.0f, ColorUtils.withAlpha(theme.getCard(), (int) (215 * alpha)));
        TextRenderer.draw(graphics, targetName, 8.0f, 4.0f, ColorUtils.withAlpha(theme.getText(), (int) (255 * alpha)), true);

        healthAnim.update();
        float shown = AnimationUtils.lerp(0.0f, targetHealthPercent, healthAnim.getValue());
        RenderUtils.fill(graphics, 8.0f, 16.0f, 114.0f, 5.0f, ColorUtils.withAlpha(0xFF000000, (int) (170 * alpha)));
        RenderUtils.roundedGradient(graphics, 8.0f, 16.0f, Math.max(3.0f, 114.0f * shown), 5.0f, 2.0f,
                theme.getPrimary(), theme.getSecondary(), false);

        float filled = shown * HEART_COUNT;
        for (int i = 0; i < HEART_COUNT; i++) {
            float hx = 8.0f + i * (HEART_W + 4.0f);
            float hy = 28.0f;
            float fill = Math.max(0.0f, Math.min(1.0f, filled - i));
            drawHeart(graphics, hx, hy, fill, alpha);
        }
    }

    /**
     * 7x6 pixel heart drawn from a static bitmask, scaled to HEART_W/HEART_H.
     * The fill parameter clips the colored columns left-to-right for a smooth
     * partial-heart transition.
     */
    private void drawHeart(GuiGraphics graphics, float x, float y, float fill, float alpha) {
        final int[] rows = {
                0b0110110,
                0b1111111,
                0b1111111,
                0b0111110,
                0b0011100,
                0b0001000
        };
        float scaleX = HEART_W / 7.0f;
        float scaleY = HEART_H / 6.0f;
        int container = ColorUtils.withAlpha(0xFF3A3A44, (int) (200 * alpha));
        int heart = ColorUtils.withAlpha(0xFFFF4655, (int) (255 * alpha));
        float fillColumns = fill * 7.0f;
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 7; c++) {
                if ((rows[r] & (1 << (6 - c))) == 0) continue;
                int color = c < fillColumns ? heart : container;
                RenderUtils.fill(graphics, x + c * scaleX, y + r * scaleY,
                        Math.max(1.0f, scaleX), Math.max(1.0f, scaleY), color);
            }
        }
    }
}
