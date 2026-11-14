package dev.lumen.client.mixin;

import dev.lumen.client.modules.visual.FullbrightModule;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Flags the lightmap rebuild window so the fullbright gamma boost only
 * applies while the light texture is regenerated - never when the value is
 * displayed in the options screen or written to options.txt.
 */
@Mixin(LightTexture.class)
public class LightTextureMixin {

    @Inject(method = "updateLightTexture(F)V", at = @At("HEAD"))
    private void lumen$beginLightmap(float partialTicks, CallbackInfo ci) {
        FullbrightModule.setLightmapUpdating(true);
    }

    @Inject(method = "updateLightTexture(F)V", at = @At("RETURN"))
    private void lumen$endLightmap(float partialTicks, CallbackInfo ci) {
        FullbrightModule.setLightmapUpdating(false);
    }
}
