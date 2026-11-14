package dev.lumen.client.mixin;

import dev.lumen.client.modules.visual.FreelookModule;
import net.minecraft.client.Camera;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overrides the camera quaternion AND the derived forwards/up/left vectors
 * with the decoupled freelook angles while the player entity's real rotation
 * stays untouched. Updating the vectors is what makes the third-person
 * camera physically orbit the player instead of staying locked behind the
 * player's head orientation.
 */
@Mixin(Camera.class)
public class CameraMixin {
    @Mutable
    @Shadow
    @Final
    private Quaternionf rotation;

    @Mutable
    @Shadow
    @Final
    private Vector3f forwards;

    @Mutable
    @Shadow
    @Final
    private Vector3f up;

    @Mutable
    @Shadow
    @Final
    private Vector3f left;

    @Shadow
    private float xRot;

    @Shadow
    private float yRot;

    @Inject(method = "setRotation(FF)V", at = @At("RETURN"))
    private void lumen$freelookRotation(float yRot, float xRot, CallbackInfo ci) {
        FreelookModule freelook = FreelookModule.get();
        if (freelook == null || !freelook.isEnabled()) return;
        if (!freelook.isActive() && !freelook.isReturning()) return;
        float yaw = freelook.getCameraYaw();
        float pitch = freelook.getCameraPitch();
        this.yRot = yaw;
        this.xRot = pitch;
        rotation.rotationYXZ(-yaw * ((float) Math.PI / 180.0f), pitch * ((float) Math.PI / 180.0f), 0.0f);
        forwards.set(0.0f, 0.0f, 1.0f).rotate(rotation);
        up.set(0.0f, 1.0f, 0.0f).rotate(rotation);
        left.set(1.0f, 0.0f, 0.0f).rotate(rotation);
    }
}
