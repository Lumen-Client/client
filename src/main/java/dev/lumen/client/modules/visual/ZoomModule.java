package dev.lumen.client.modules.visual;

import dev.lumen.client.LumenClient;
import dev.lumen.client.anim.Animation;
import dev.lumen.client.anim.Easing;
import dev.lumen.client.core.Module;
import dev.lumen.client.core.Category;
import dev.lumen.client.core.setting.BooleanSetting;
import dev.lumen.client.core.setting.EnumSetting;
import dev.lumen.client.core.setting.NumberSetting;

/**
 * Smooth cinematic zoom. FOV changes are applied through the GameRenderer
 * mixin; scroll magnification and sensitivity scaling through MouseHandler.
 */
public class ZoomModule extends Module {
    public enum Mode { HOLD, TOGGLE }

    private static ZoomModule instance;

    public final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.class, Mode.HOLD);
    public final NumberSetting magnification = new NumberSetting("Magnification", 4.0, 1.5, 50.0, 0.5, 1);
    public final NumberSetting scrollStep = new NumberSetting("Scroll Step", 0.5, 0.1, 3.0, 0.1, 1);
    public final NumberSetting smoothness = new NumberSetting("Smoothness", 0.65, 0.1, 1.0, 0.05, 2);
    public final BooleanSetting smoothCamera = new BooleanSetting("Cinematic Camera", true);
    public final BooleanSetting hideHand = new BooleanSetting("Hide Hand", false);
    public final BooleanSetting hideHud = new BooleanSetting("Hide HUD", false);

    private final Animation zoomAnim = new Animation(0.35f, Easing::easeOutCubic);
    private double targetMagnification;
    private double currentMagnification;
    private boolean zooming;
    private boolean wasSmoothCamera;

    public ZoomModule() {
        super("Zoom", "Smooth zoom with scroll magnification", Category.VISUALS);
        instance = this;
        addSettings(mode, magnification, scrollStep, smoothness, smoothCamera, hideHand, hideHud);
        targetMagnification = magnification.get();
        currentMagnification = targetMagnification;
    }

    public static ZoomModule get() {
        return instance;
    }

    public boolean isZooming() {
        return zooming && zoomAnim.getValue() > 0.001f;
    }

    public float getZoomFactor() {
        float activate = zoomAnim.getValue();
        if (activate <= 0.0001f) return 1.0f;
        double mag = currentMagnification;
        float eased = 1.0f - activate;
        return (float) (1.0 + (mag - 1.0) * Math.pow(eased, 1.6));
    }

    public double getCurrentMagnification() {
        return currentMagnification;
    }

    public void scrollMagnification(double direction) {
        targetMagnification = Math.max(1.5, Math.min(50.0, targetMagnification - direction * scrollStep.get()));
    }

    @Override
    public void onClientTick() {
        var keybinds = LumenClient.get().getKeybindManager();
        boolean activate;
        if (mode.get() == Mode.HOLD) {
            activate = keybinds.isZoomDown();
            while (keybinds.consumeZoomClick()) {
                // drain clicks in hold mode
            }
        } else {
            while (keybinds.consumeZoomClick()) {
                zooming = !zooming;
            }
            activate = zooming;
        }
        zooming = activate;

        if (activate && !zoomAnim.isTargetOn()) {
            if (smoothCamera.get()) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                wasSmoothCamera = mc.options.smoothCamera;
                mc.options.smoothCamera = true;
            }
            zoomAnim.setTarget(true);
        } else if (!activate && zoomAnim.isTargetOn()) {
            if (smoothCamera.get() && !wasSmoothCamera) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                mc.options.smoothCamera = false;
            }
            zoomAnim.setTarget(false);
        }
    }

    @Override
    public void onRenderTick() {
        zoomAnim.update();
        currentMagnification = dev.lumen.client.anim.AnimationUtils.approach(
                (float) currentMagnification, (float) targetMagnification, 12.0f, 0.05f);
    }

    @Override
    public void onDisable() {
        zooming = false;
        zoomAnim.setTarget(false);
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (smoothCamera.get() && !wasSmoothCamera) {
            mc.options.smoothCamera = false;
        }
    }
}
