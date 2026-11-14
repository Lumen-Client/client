package dev.lumen.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.lumen.client.modules.visual.LowFireModule;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Compresses the first-person fire overlay. In 1.21.11 the fire overlay is
 * drawn by ScreenEffectRenderer.renderFire as two camera-space quads spanning
 * y in [-0.8, 0.2]; scaling the pose towards the quad bottom squashes the
 * flames down to the configured screen fraction.
 */
@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {

    @Inject(method = "renderFire", at = @At("HEAD"))
    private static void lumen$lowFire(PoseStack poseStack, MultiBufferSource bufferSource, TextureAtlasSprite sprite, CallbackInfo ci) {
        LowFireModule module = LowFireModule.get();
        if (module == null || !module.isEnabled()) return;
        float fraction = Math.max(0.05f, Math.min(1.0f, module.getFireHeightFraction()));
        poseStack.translate(0.0f, -0.8f * (1.0f - fraction), 0.0f);
        poseStack.scale(1.0f, fraction, 1.0f);
    }
}
