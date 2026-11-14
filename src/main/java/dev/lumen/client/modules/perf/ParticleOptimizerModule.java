package dev.lumen.client.modules.perf;

import dev.lumen.client.core.Module;
import dev.lumen.client.core.Category;
import dev.lumen.client.core.setting.BooleanSetting;
import dev.lumen.client.core.setting.NumberSetting;

public class ParticleOptimizerModule extends Module {
    private static ParticleOptimizerModule instance;

    public final NumberSetting maxDistance = new NumberSetting("Max Distance", 32.0, 8.0, 128.0, 1.0, 0);
    public final NumberSetting particleCap = new NumberSetting("Particle Cap", 2000, 500, 4000, 100, 0);
    public final BooleanSetting suppressExplosions = new BooleanSetting("Suppress Explosions", false);
    public final BooleanSetting suppressPotionSwirls = new BooleanSetting("Suppress Potion Swirls", false);
    public final BooleanSetting suppressBlockDust = new BooleanSetting("Suppress Block Dust", false);
    public final BooleanSetting suppressRainSplash = new BooleanSetting("Suppress Rain Splash", false);
    public final BooleanSetting protectCombatParticles = new BooleanSetting("Protect Crit Particles", true);

    private int liveCount;

    public ParticleOptimizerModule() {
        super("Particle Optimizer", "Frustum, distance and cap culling for particles", Category.PERFORMANCE);
        instance = this;
        addSettings(maxDistance, particleCap, suppressExplosions, suppressPotionSwirls,
                suppressBlockDust, suppressRainSplash, protectCombatParticles);
    }

    public static ParticleOptimizerModule get() {
        return instance;
    }

    public boolean canAddParticle() {
        if (!isEnabled()) return true;
        return liveCount < particleCap.getInt();
    }

    public void onParticleAdded() {
        liveCount++;
    }

    public void onParticleRemoved() {
        if (liveCount > 0) liveCount--;
    }

    public void resetCount() {
        liveCount = 0;
    }

    public double getMaxDistanceSq() {
        double d = isEnabled() ? maxDistance.get() : Double.MAX_VALUE;
        return d * d;
    }

    public boolean shouldSuppress(net.minecraft.core.particles.ParticleOptions options) {
        String path = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE
                .getKey(options.getType()).getPath();
        if (protectCombatParticles.get()
                && (path.equals("crit") || path.equals("damage_indicator")
                || path.equals("enchanted_hit") || path.equals("sweep_attack"))) {
            return false;
        }
        if (suppressExplosions.get() && (path.equals("explosion") || path.equals("explosion_emitter"))) return true;
        if (suppressPotionSwirls.get() && (path.equals("entity_effect") || path.equals("effect")
                || path.equals("witch") || path.equals("instant_effect"))) return true;
        if (suppressBlockDust.get() && (path.equals("block") || path.equals("block_marker")
                || path.equals("falling_dust"))) return true;
        if (suppressRainSplash.get() && (path.equals("rain") || path.equals("splash")
                || path.equals("falling_water"))) return true;
        return false;
    }
}
