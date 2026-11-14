package dev.lumen.client.modules.perf;

import dev.lumen.client.core.Module;
import dev.lumen.client.core.Category;
import dev.lumen.client.core.setting.BooleanSetting;
import dev.lumen.client.core.setting.NumberSetting;

public class EntityOptimizerModule extends Module {
    private static EntityOptimizerModule instance;

    public final NumberSetting maxRenderDistance = new NumberSetting("Max Render Distance", 64.0, 16.0, 256.0, 4.0, 0);
    public final NumberSetting itemMergeDistance = new NumberSetting("Item Cull Distance", 32.0, 8.0, 128.0, 4.0, 0);
    public final BooleanSetting cullBlockEntities = new BooleanSetting("Cull Block Entities", true);
    public final NumberSetting blockEntityDistance = new NumberSetting("Block Entity Distance", 48.0, 16.0, 128.0, 4.0, 0);
    public final BooleanSetting throttleDistantTicks = new BooleanSetting("Throttle Distant Ticks", false);
    public final NumberSetting throttleDistance = new NumberSetting("Throttle Distance", 48.0, 16.0, 128.0, 4.0, 0);

    public EntityOptimizerModule() {
        super("Entity Optimizer", "Distance and occlusion culling for entities", Category.PERFORMANCE);
        instance = this;
        addSettings(maxRenderDistance, itemMergeDistance, cullBlockEntities, blockEntityDistance,
                throttleDistantTicks, throttleDistance);
    }

    public static EntityOptimizerModule get() {
        return instance;
    }

    public boolean shouldCullEntity(double distanceSq) {
        if (!isEnabled()) return false;
        double max = maxRenderDistance.get();
        return distanceSq > max * max;
    }

    public double getItemCullDistanceSq() {
        double d = isEnabled() ? itemMergeDistance.get() : Double.MAX_VALUE;
        return d * d;
    }

    public boolean shouldCullBlockEntity(double distanceSq) {
        if (!isEnabled() || !cullBlockEntities.get()) return false;
        double d = blockEntityDistance.get();
        return distanceSq > d * d;
    }
}
