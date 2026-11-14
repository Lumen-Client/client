package dev.lumen.client.modules.perf;

import dev.lumen.client.core.Module;
import dev.lumen.client.core.Category;
import dev.lumen.client.core.setting.BooleanSetting;
import dev.lumen.client.core.setting.NumberSetting;

public class ParticleOptimizerModule extends Module {
    private static ParticleOptimizerModule instance;

    private static final String[][] GROUPS = {
            {"crit", "damage_indicator", "enchanted_hit", "sweep_attack"},
            {"explosion", "explosion_emitter"},
            {"entity_effect", "effect", "witch", "instant_effect"},
            {"block", "block_marker", "falling_dust"},
            {"rain", "splash", "falling_water"},
            {"smoke", "large_smoke", "white_smoke", "campfire_cosy_smoke", "campfire_signal_smoke"},
            {"flame", "small_flame", "lava", "soul_fire_flame"},
            {"heart", "note"},
            {"portal", "reverse_portal", "end_rod", "dragon_breath"},
            {"enchant", "glow"},
            {"firework", "spark"},
            {"bubble", "bubble_pop", "bubble_column_up"},
            {"cloud", "white_ash"},
            {"cherry_leaves", "pale_oak_leaves"},
            {"ash", "spore_blossom_air"}
    };

    private final NumberSetting maxDistance = new NumberSetting("Max Distance", 32.0, 8.0, 128.0, 1.0, 0);
    private final NumberSetting particleCap = new NumberSetting("Particle Cap", 2000, 500, 4000, 100, 0);
    private final BooleanSetting advancedMode = new BooleanSetting("Advanced Mode", false);
    private final BooleanSetting protectCombatParticles = new BooleanSetting("Protect Crit Particles", true);

    private final BooleanSetting suppressExplosions = new BooleanSetting("Suppress Explosions", false);
    private final BooleanSetting suppressPotionSwirls = new BooleanSetting("Suppress Potion Swirls", false);
    private final BooleanSetting suppressBlockDust = new BooleanSetting("Suppress Block Dust", false);
    private final BooleanSetting suppressRainSplash = new BooleanSetting("Suppress Rain Splash", false);

    private final BooleanSetting[] groupToggles = new BooleanSetting[GROUPS.length];

    private int liveCount;

    public ParticleOptimizerModule() {
        super("Particle Optimizer", "Frustum, distance and cap culling for particles", Category.PERFORMANCE);
        instance = this;
        for (int i = 0; i < GROUPS.length; i++) {
            groupToggles[i] = new BooleanSetting("Allow: " + groupLabel(GROUPS[i]), true);
            groupToggles[i].setVisibleWhen(advancedMode::get);
        }
        suppressExplosions.setVisibleWhen(() -> !advancedMode.get());
        suppressPotionSwirls.setVisibleWhen(() -> !advancedMode.get());
        suppressBlockDust.setVisibleWhen(() -> !advancedMode.get());
        suppressRainSplash.setVisibleWhen(() -> !advancedMode.get());
        addSettings(maxDistance, particleCap, advancedMode, protectCombatParticles);
        addSettings(suppressExplosions, suppressPotionSwirls, suppressBlockDust, suppressRainSplash);
        for (BooleanSetting toggle : groupToggles) {
            addSettings(toggle);
        }
    }

    private static String groupLabel(String[] paths) {
        return switch (paths[0]) {
            case "crit" -> "Combat (Crits)";
            case "explosion" -> "Explosions";
            case "entity_effect" -> "Potion Swirls";
            case "block" -> "Block Dust";
            case "rain" -> "Rain & Splash";
            case "smoke" -> "Smoke";
            case "flame" -> "Fire & Lava";
            case "heart" -> "Hearts & Notes";
            case "portal" -> "Portal & End";
            case "enchant" -> "Enchant & Glow";
            case "firework" -> "Fireworks";
            case "bubble" -> "Bubbles";
            case "cloud" -> "Clouds & Ash";
            case "cherry_leaves" -> "Falling Leaves";
            default -> "Ambient Spores";
        };
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

    private static boolean matches(String[] group, String path) {
        for (String entry : group) {
            if (entry.equals(path)) return true;
        }
        return false;
    }

    public boolean shouldSuppress(net.minecraft.core.particles.ParticleOptions options) {
        String path = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE
                .getKey(options.getType()).getPath();
        if (protectCombatParticles.get() && matches(GROUPS[0], path)) {
            return false;
        }
        if (advancedMode.get()) {
            for (int i = 1; i < GROUPS.length; i++) {
                if (matches(GROUPS[i], path)) {
                    return !groupToggles[i].get();
                }
            }
            return false;
        }
        if (suppressExplosions.get() && matches(GROUPS[1], path)) return true;
        if (suppressPotionSwirls.get() && matches(GROUPS[2], path)) return true;
        if (suppressBlockDust.get() && matches(GROUPS[3], path)) return true;
        if (suppressRainSplash.get() && matches(GROUPS[4], path)) return true;
        return false;
    }
}
