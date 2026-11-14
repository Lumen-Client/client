package dev.lumen.client.mixin;

import dev.lumen.client.modules.visual.FreelookModule;
import net.minecraft.client.Camera;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overrides the camera quaternion with the decoupled freelook angles while
 * the player entity's real rotation stays untouched.
 */
@Mixin(Camera.class)
public class CameraMixin {
    @Mutable
    @Shadow
    @Final
    private Quaternionf rotation;

    @Inject(method = "setRotation(FF)V", at = @At("RETURN"))
    private void lumen$freelookRotation(float yRot, float xRot, CallbackInfo ci) {
        FreelookModule freelook = FreelookModule.get();
        if (freelook == null || !freelook.isEnabled()) return;
        if (!freelook.isActive() && !freelook.isReturning()) return;
        float yaw = freelook.getCameraYaw();
        float pitch = freelook.getCameraPitch();
        rotation.rotationYXZ(-yaw * ((float) Math.PI / 180.0f), pitch * ((float) Math.PI / 180.0f), 0.0f);
    }
}
