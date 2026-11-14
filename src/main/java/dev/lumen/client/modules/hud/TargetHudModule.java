package dev.lumen.client.modules.hud;

import dev.lumen.client.anim.Animation;
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
    private final Animation showAnim = new Animation(0.2f, Easing::easeOutCubic);
    private final Animation healthAnim = new Animation(0.25f, Easing::easeOutCubic);
    private String targetName = "";
    private float targetHealthPercent;

    public TargetHudModule() {
        super("Target HUD", "Health bar of the entity you are looking at");
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
        return 34.0f;
    }

    @Override
    public void renderHud(GuiGraphics graphics, float delta) {
        showAnim.update();
        float alpha = showAnim.getValue();
        if (alpha < 0.02f) return;
        ThemeManager theme = ThemeManager.get();

        RenderUtils.roundedRect(graphics, 0.0f, 0.0f, 130.0f, 34.0f, 5.0f, ColorUtils.withAlpha(theme.getCard(), (int) (215 * alpha)));
        TextRenderer.draw(graphics, targetName, 8.0f, 4.0f, ColorUtils.withAlpha(theme.getText(), (int) (255 * alpha)), true);

        healthAnim.update();
        float shown = dev.lumen.client.anim.AnimationUtils.lerp(0.0f, targetHealthPercent, healthAnim.getValue());
        RenderUtils.fill(graphics, 8.0f, 22.0f, 114.0f, 5.0f, ColorUtils.withAlpha(0xFF000000, (int) (170 * alpha)));
        RenderUtils.roundedGradient(graphics, 8.0f, 22.0f, Math.max(3.0f, 114.0f * shown), 5.0f, 2.0f,
                theme.getPrimary(), theme.getSecondary(), false);
    }

}
