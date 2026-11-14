package dev.lumen.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.lumen.client.modules.visual.LowShieldModule;
import dev.lumen.client.modules.visual.NoHurtCamModule;
import dev.lumen.client.modules.visual.ZoomModule;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Zoom FOV interpolation, hide-hand while zoomed, and No Hurt Cam.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @ModifyReturnValue(method = "getFov(Lnet/minecraft/client/Camera;FZ)F", at = @At("RETURN"))
    private float lumen$zoomFov(float original, Camera camera, float partialTick, boolean useFovSetting) {
        ZoomModule zoom = ZoomModule.get();
        if (zoom != null && zoom.isEnabled() && zoom.getZoomFactor() > 1.0f) {
            return original / zoom.getZoomFactor();
        }
        return original;
    }

    @Inject(method = "renderItemInHand(FZLorg/joml/Matrix4f;)V", at = @At("HEAD"), cancellable = true)
    private void lumen$hideHand(float partialTick, boolean bl, Matrix4f matrix4f, CallbackInfo ci) {
        ZoomModule zoom = ZoomModule.get();
        if (zoom != null && zoom.isEnabled() && zoom.hideHand.get() && zoom.isZooming()) {
            ci.cancel();
        }
    }

    @Inject(method = "bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V", at = @At("HEAD"), cancellable = true)
    private void lumen$noHurtCam(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        NoHurtCamModule module = NoHurtCamModule.get();
        if (module != null && module.shouldCancel()) {
            ci.cancel();
        }
    }
}
