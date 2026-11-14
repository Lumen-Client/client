package dev.lumen.client.modules.visual;

import dev.lumen.client.anim.Animation;
import dev.lumen.client.anim.Easing;
import dev.lumen.client.core.Module;
import dev.lumen.client.core.Category;
import dev.lumen.client.core.setting.BooleanSetting;

public class FullbrightModule extends Module {
    private static FullbrightModule instance;
    private static volatile boolean lightmapUpdating;

    public final BooleanSetting smoothTransition = new BooleanSetting("Smooth Transition", true);

    private final Animation gammaAnim = new Animation(0.6f, Easing::easeInOutQuart);

    public FullbrightModule() {
        super("Fullbright", "Smooth gamma boost for caves and night", Category.VISUALS);
        instance = this;
        addSettings(smoothTransition);
    }

    public static FullbrightModule get() {
        return instance;
    }

    /**
     * Only true while the lightmap texture is being rebuilt, so the boosted
     * gamma never leaks into the options screen or the saved options.txt
     * (which previously made fullbright appear permanently stuck on).
     */
    public static boolean isLightmapUpdating() {
        return lightmapUpdating;
    }

    public static void setLightmapUpdating(boolean updating) {
        lightmapUpdating = updating;
    }

    public float getGammaBoost() {
        if (!isEnabled()) return 0.0f;
        if (!smoothTransition.get()) {
            gammaAnim.snap(true);
            return 1.0f;
        }
        gammaAnim.update();
        return gammaAnim.getValue();
    }

    @Override
    public void onEnable() {
        gammaAnim.setTarget(true);
    }

    @Override
    public void onDisable() {
        gammaAnim.setTarget(false);
    }
}
