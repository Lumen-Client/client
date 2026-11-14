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

/**
 * Pure vanilla-state indicator. Reads the native sprint/sneak key state and
 * the vanilla accessibility toggle options. Never modifies movement.
 */
public class SprintStatusModule extends HudModule {
    private final Animation showAnim = new Animation(0.18f, Easing::easeOutBack);
    private final dev.lumen.client.core.setting.BooleanSetting showSneak =
            new dev.lumen.client.core.setting.BooleanSetting("Show Sneak", true);

    private String label = "";
    private int labelColor;
    private boolean visible;

    public SprintStatusModule() {
        super("Sprint Status", "Vanilla sprint/sneak state badge (read-only)", 6.0f, 210.0f);
        addSettings(showSneak);
    }

    @Override
    public void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            visible = false;
            return;
        }
        boolean sprintToggled = mc.options.toggleSprint().get();
        boolean sneakToggled = mc.options.toggleCrouch().get();
        boolean sprinting = mc.options.keySprint.isDown() || (sprintToggled && mc.player.isSprinting());
        boolean sneaking = mc.options.keyShift.isDown() || (sneakToggled && mc.player.isShiftKeyDown());

        if (sneaking && showSneak.get()) {
            label = sneakToggled && mc.player.isShiftKeyDown() ? "[Sneak (Toggled)]" : "[Sneak (Held)]";
            labelColor = 0xFF7ED0FF;
            visible = true;
        } else if (sprinting) {
            label = sprintToggled && mc.player.isSprinting() ? "[Sprint (Toggled)]" : "[Sprint (Held)]";
            labelColor = ThemeManager.get().getPrimary();
            visible = true;
        } else {
            visible = false;
        }
        showAnim.setTarget(visible);
    }

    @Override
    public float getHudWidth() {
        return Math.max(TextRenderer.width(label) + 10.0f, 60.0f);
    }

    @Override
    public float getHudHeight() {
        return TextRenderer.height() + 8.0f;
    }

    @Override
    public void renderHud(GuiGraphics graphics, float delta) {
        showAnim.update();
        float alpha = showAnim.getValue();
        if (alpha < 0.02f) return;
        ThemeManager theme = ThemeManager.get();
        int bg = ColorUtils.withAlpha(theme.getBackground(), (int) (200 * alpha));
        RenderUtils.roundedRect(graphics, 0.0f, 0.0f, getHudWidth(), getHudHeight(), 5.0f, bg);
        TextRenderer.draw(graphics, label, 5.0f, 4.0f, ColorUtils.withAlpha(labelColor, (int) (255 * alpha)), true);
    }
}
