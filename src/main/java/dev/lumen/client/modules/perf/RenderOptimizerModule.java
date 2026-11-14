package dev.lumen.client.modules.perf;

import dev.lumen.client.core.Module;
import dev.lumen.client.core.Category;
import dev.lumen.client.core.setting.BooleanSetting;
import dev.lumen.client.core.setting.NumberSetting;

public class RenderOptimizerModule extends Module {
    private static RenderOptimizerModule instance;

    public final BooleanSetting unfocusedLimiter = new BooleanSetting("Unfocused FPS Limiter", true);
    public final NumberSetting unfocusedFps = new NumberSetting("Unfocused FPS", 15, 5, 30, 1, 0);

    public RenderOptimizerModule() {
        super("Render Optimizer", "HUD caching, unfocused FPS limiter, fast transitions", Category.PERFORMANCE);
        instance = this;
        addSettings(unfocusedLimiter, unfocusedFps);
    }

    public static RenderOptimizerModule get() {
        return instance;
    }

    public int getUnfocusedFpsLimit(int vanillaLimit) {
        if (!isEnabled() || !unfocusedLimiter.get()) return vanillaLimit;
        return Math.min(vanillaLimit, unfocusedFps.getInt());
    }
}
