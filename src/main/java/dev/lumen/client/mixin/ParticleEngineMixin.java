package dev.lumen.client.mixin;

import dev.lumen.client.modules.perf.ParticleOptimizerModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Particle cap, distance culling and high-spam particle suppression.
 */
@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void lumen$particleCap(Particle particle, CallbackInfo ci) {
        ParticleOptimizerModule module = ParticleOptimizerModule.get();
        if (module == null || !module.isEnabled()) return;
        if (!module.canAddParticle()) {
            ci.cancel();
        }
    }

    @Inject(method = "add", at = @At("TAIL"))
    private void lumen$particleAdded(Particle particle, CallbackInfo ci) {
        ParticleOptimizerModule module = ParticleOptimizerModule.get();
        if (module != null) {
            module.onParticleAdded();
        }
    }

    @Inject(method = "makeParticle", at = @At("HEAD"), cancellable = true)
    private <T extends ParticleOptions> void lumen$distanceCull(T options, double x, double y, double z,
                                                                double xSpeed, double ySpeed, double zSpeed,
                                                                CallbackInfoReturnable<Particle> cir) {
        ParticleOptimizerModule module = ParticleOptimizerModule.get();
        if (module == null || !module.isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        double distSq = mc.player.distanceToSqr(x, y, z);
        if (distSq > module.getMaxDistanceSq() || module.shouldSuppress(options)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "clearParticles", at = @At("HEAD"))
    private void lumen$cleared(CallbackInfo ci) {
        ParticleOptimizerModule module = ParticleOptimizerModule.get();
        if (module != null) {
            module.resetCount();
        }
    }
}
