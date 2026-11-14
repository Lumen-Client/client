package dev.lumen.client.modules.perf;

import dev.lumen.client.core.Module;
import dev.lumen.client.core.Category;
import dev.lumen.client.core.setting.BooleanSetting;

public class SystemOptimizerModule extends Module {
    private static SystemOptimizerModule instance;

    public final BooleanSetting fastTrig = new BooleanSetting("Fast Trig Table", true);
    public final BooleanSetting showRamBar = new BooleanSetting("Show RAM Bar", true);

    public SystemOptimizerModule() {
        super("System Optimizer", "Fast trig, memory monitor and GC trigger", Category.PERFORMANCE);
        instance = this;
        addSettings(fastTrig, showRamBar);
    }

    public static SystemOptimizerModule get() {
        return instance;
    }

    public boolean useFastTrig() {
        return isEnabled() && fastTrig.get();
    }

    public float getUsedHeapFraction() {
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        return total == 0 ? 0.0f : (float) ((double) (total - free) / total);
    }

    public float getAllocatedFraction() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long total = runtime.totalMemory();
        return max == 0 ? 0.0f : (float) ((double) total / max);
    }

    public void triggerCleanup() {
        System.gc();
    }
}
