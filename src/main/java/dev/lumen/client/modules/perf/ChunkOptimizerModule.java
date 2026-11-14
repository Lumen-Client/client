package dev.lumen.client.modules.perf;

import dev.lumen.client.core.Module;
import dev.lumen.client.core.Category;
import dev.lumen.client.core.setting.NumberSetting;

public class ChunkOptimizerModule extends Module {
    private static ChunkOptimizerModule instance;

    public final NumberSetting rebuildBudgetMs = new NumberSetting("Rebuild Budget (ms)", 4.0, 1.0, 15.0, 0.5, 1);
    public final NumberSetting maxCompileQueue = new NumberSetting("Max Compile Queue", 64, 8, 256, 8, 0);

    private long frameStartNanos;
    private long budgetUsedNanos;

    public ChunkOptimizerModule() {
        super("Chunk Optimizer", "Frame-budgeted chunk rebuilds and uploads", Category.PERFORMANCE);
        instance = this;
        addSettings(rebuildBudgetMs, maxCompileQueue);
    }

    public static ChunkOptimizerModule get() {
        return instance;
    }

    public boolean hasUploadBudget() {
        if (!isEnabled()) return true;
        long now = System.nanoTime();
        if (now - frameStartNanos > 50_000_000L) {
            frameStartNanos = now;
            budgetUsedNanos = 0;
        }
        return budgetUsedNanos < (long) (rebuildBudgetMs.getFloat() * 1_000_000.0f);
    }

    public void onUploadSpent(long nanos) {
        budgetUsedNanos += nanos;
    }

    public int getMaxCompileQueue() {
        return isEnabled() ? maxCompileQueue.getInt() : Integer.MAX_VALUE;
    }
}
