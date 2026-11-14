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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class TargetHudModule extends HudModule {
    private static final Identifier HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.withDefaultNamespace("hud/heart/half");
    private static final Identifier HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");
    private static final float HEART_SIZE = 9.0f;
    private static final float HEART_STEP = 10.0f;
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
    public void preRender() {
        showAnim.update();
        healthAnim.update();
    }

    @Override
    public void renderHud(GuiGraphics graphics, float delta) {
        float alpha = showAnim.getValue();
        if (alpha < 0.02f) return;
        ThemeManager theme = ThemeManager.get();

        RenderUtils.roundedRect(graphics, 0.0f, 0.0f, 130.0f, 46.0f, 5.0f, ColorUtils.withAlpha(theme.getCard(), (int) (215 * alpha)));
        TextRenderer.draw(graphics, targetName, 8.0f, 5.0f, ColorUtils.withAlpha(theme.getText(), (int) (255 * alpha)), true);

        float shown = AnimationUtils.lerp(0.0f, targetHealthPercent, healthAnim.getValue());
        RenderUtils.fill(graphics, 8.0f, 17.0f, 114.0f, 5.0f, ColorUtils.withAlpha(0xFF000000, (int) (170 * alpha)));
        RenderUtils.fill(graphics, 8.0f, 17.0f, Math.max(3.0f, 114.0f * shown), 5.0f,
                ColorUtils.withAlpha(theme.getPrimary(), (int) (255 * alpha)));

        float filled = shown * HEART_COUNT;
        for (int i = 0; i < HEART_COUNT; i++) {
            int hx = (int) (8.0f + i * HEART_STEP);
            int hy = 29;
            float fill = Math.max(0.0f, Math.min(1.0f, filled - i));
            Identifier sprite = fill >= 0.5f ? HEART_FULL : fill > 0.0f ? HEART_HALF : HEART_CONTAINER;
            graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, sprite, hx, hy,
                    (int) HEART_SIZE, (int) HEART_SIZE);
        }
    }
}
