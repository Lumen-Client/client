package dev.lumen.client.modules.visual;

import dev.lumen.client.core.Category;
import dev.lumen.client.core.Module;
import dev.lumen.client.core.setting.NumberSetting;

/**
 * Lowers the first-person shield while blocking so it covers less of the
 * screen. The offset is applied to the hand render matrix via the
 * GameRenderer mixin.
 */
public class LowShieldModule extends Module {
    private static LowShieldModule instance;

    public final NumberSetting offset = new NumberSetting("Lower Amount", 0.25, 0.05, 0.6, 0.05, 2);

    public LowShieldModule() {
        super("Low Shield", "Lowers the raised shield so it blocks less of your view", Category.VISUALS);
        instance = this;
        addSettings(offset);
    }

    public static LowShieldModule get() {
        return instance;
    }

    public float getOffset() {
        return offset.getFloat();
    }
}
