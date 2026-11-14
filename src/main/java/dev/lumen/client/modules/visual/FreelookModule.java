package dev.lumen.client.modules.visual;

import dev.lumen.client.LumenClient;
import dev.lumen.client.anim.AnimationUtils;
import dev.lumen.client.anim.AnimationUtils.VelocityState;
import dev.lumen.client.core.Module;
import dev.lumen.client.core.Category;
import dev.lumen.client.core.setting.BooleanSetting;
import dev.lumen.client.core.setting.EnumSetting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

/**
 * Freelook with fully decoupled camera rotation. Player rotation is never
 * touched - mouse deltas are redirected to cameraYaw/cameraPitch while the
 * mixin chain keeps the entity's server-synced rotation locked.
 */
public class FreelookModule extends Module {
    public enum Mode { HOLD, TOGGLE }
    public enum Perspective { THIRD_PERSON_BACK, THIRD_PERSON_FRONT }

    private static FreelookModule instance;

    public final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.class, Mode.HOLD);
    public final EnumSetting<Perspective> perspective = new EnumSetting<>("Perspective", Perspective.class, Perspective.THIRD_PERSON_BACK);
    public final BooleanSetting invertHorizontal = new BooleanSetting("Invert Horizontal", false);
    public final BooleanSetting invertVertical = new BooleanSetting("Invert Vertical", false);
    public final BooleanSetting smoothReturn = new BooleanSetting("Smooth Snap-Back", true);

    private float cameraYaw;
    private float cameraPitch;
    private final VelocityState yawVelocity = new VelocityState();
    private final VelocityState pitchVelocity = new VelocityState();

    private boolean active;
    private boolean returning;
    private CameraType previousCameraType = CameraType.FIRST_PERSON;

    public FreelookModule() {
        super("Freelook", "360 degree camera without rotating the player", Category.VISUALS);
        instance = this;
        addSettings(mode, perspective, invertHorizontal, invertVertical, smoothReturn);
    }

    public static FreelookModule get() {
        return instance;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isReturning() {
        return returning;
    }

    public float getCameraYaw() {
        return cameraYaw;
    }

    public float getCameraPitch() {
        return cameraPitch;
    }

    public void addLookDelta(double yawDelta, double pitchDelta) {
        float sensitivity = 0.22f;
        cameraYaw += (float) yawDelta * sensitivity * (invertHorizontal.get() ? -1.0f : 1.0f);
        cameraPitch += (float) pitchDelta * sensitivity * (invertVertical.get() ? -1.0f : 1.0f);
        cameraYaw = net.minecraft.util.Mth.wrapDegrees(cameraYaw);
        cameraPitch = Math.max(-90.0f, Math.min(90.0f, cameraPitch));
    }

    @Override
    public void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        var keybinds = LumenClient.get().getKeybindManager();
        boolean activate;
        if (mode.get() == Mode.HOLD) {
            activate = keybinds.isFreelookDown();
            while (keybinds.consumeFreelookClick()) {
            }
        } else {
            while (keybinds.consumeFreelookClick()) {
                active = !active;
            }
            activate = active;
        }

        if (activate && !active) {
            start(mc);
        } else if (!activate && active) {
            beginReturn(mc);
        }
    }

    private void start(Minecraft mc) {
        active = true;
        returning = false;
        CameraType current = mc.options.getCameraType();
        if (current != CameraType.THIRD_PERSON_BACK && current != CameraType.THIRD_PERSON_FRONT) {
            previousCameraType = current;
        }
        CameraType target = perspective.get() == Perspective.THIRD_PERSON_FRONT
                ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK;
        mc.options.setCameraType(target);
        cameraYaw = mc.player.getYRot();
        cameraPitch = mc.player.getXRot();
        yawVelocity.velocity = 0.0f;
        pitchVelocity.velocity = 0.0f;
    }

    private void beginReturn(Minecraft mc) {
        active = false;
        if (smoothReturn.get() && mc.player != null) {
            returning = true;
        } else {
            finishReturn(mc);
        }
    }

    private void finishReturn(Minecraft mc) {
        returning = false;
        mc.options.setCameraType(previousCameraType);
    }

    @Override
    public void onRenderTick() {
        Minecraft mc = Minecraft.getInstance();
        if (returning && mc.player != null) {
            float delta = 0.06f;
            float targetYaw = mc.player.getYRot();
            float targetPitch = mc.player.getXRot();
            float yawDiff = net.minecraft.util.Mth.wrapDegrees(targetYaw - cameraYaw);
            cameraYaw = AnimationUtils.springApproach(cameraYaw, cameraYaw + yawDiff, yawVelocity, delta);
            cameraPitch = AnimationUtils.springApproach(cameraPitch, targetPitch, pitchVelocity, delta);
            if (Math.abs(yawDiff) < 0.15f && Math.abs(cameraPitch - targetPitch) < 0.15f) {
                cameraYaw = targetYaw;
                cameraPitch = targetPitch;
                finishReturn(mc);
            }
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        active = false;
        returning = false;
        if (mc.options.getCameraType() != previousCameraType && mc.player != null) {
            mc.options.setCameraType(previousCameraType);
        }
    }
}
