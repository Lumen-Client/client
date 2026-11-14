package dev.lumen.client.mixin;

import dev.lumen.client.modules.perf.ParticleOptimizerModule;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the particle optimizer's live count in sync with particle removal.
 */
@Mixin(Particle.class)
public class ParticleMixin {
    @Shadow
    protected boolean removed;

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void lumen$removed(CallbackInfo ci) {
        if (removed) {
            ci.cancel();
            return;
        }
        ParticleOptimizerModule module = ParticleOptimizerModule.get();
        if (module != null) {
            module.onParticleRemoved();
        }
    }
}
