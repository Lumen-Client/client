package dev.lumen.client.modules.hud;

import dev.lumen.client.anim.Animation;
import dev.lumen.client.anim.Easing;
import dev.lumen.client.core.HudModule;
import dev.lumen.client.render.TextRenderer;
import dev.lumen.client.theme.ColorUtils;
import dev.lumen.client.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class ComboCounterModule extends HudModule {
    private final Animation popAnim = new Animation(0.35f, Easing::spring);
    private int combo;
    private long lastHitMillis;
    private String displayText = "";

    public ComboCounterModule() {
        super("Combo Counter", "Consecutive hits landed");
    }

    @Override
    public void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            combo = 0;
            return;
        }
        if (combo > 0 && System.currentTimeMillis() - lastHitMillis > 2500) {
            combo = 0;
        }
        Entity target = mc.crosshairPickEntity;
        if (target instanceof LivingEntity && mc.mouseHandler.isLeftPressed()) {
            registerHit();
        }
        displayText = combo > 0 ? combo + " Combo" : "";
        popAnim.setTarget(true);
    }

    public void registerHit() {
        combo++;
        lastHitMillis = System.currentTimeMillis();
        popAnim.snap(false);
        popAnim.setTarget(true);
    }

    @Override
    public float getHudWidth() {
        return Math.max(TextRenderer.width(displayText) + 10.0f, 50.0f);
    }

    @Override
    public float getHudHeight() {
        return TextRenderer.height() + 8.0f;
    }

    @Override
    public void renderHud(GuiGraphics graphics, float delta) {
        if (displayText.isEmpty()) return;
        popAnim.update();
        float pop = 1.0f + 0.25f * (1.0f - popAnim.getValue());
        ThemeManager theme = ThemeManager.get();
        TextRenderer.drawScaled(graphics, displayText, 5.0f, 4.0f + (pop - 1.0f) * 4.0f,
                ColorUtils.withAlpha(theme.getPrimary(), 255), pop, true);
    }
}
