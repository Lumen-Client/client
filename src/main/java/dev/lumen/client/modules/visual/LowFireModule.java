package dev.lumen.client.modules.visual;

import dev.lumen.client.core.Module;
import dev.lumen.client.core.Category;
import dev.lumen.client.core.setting.NumberSetting;

/**
 * Low fire overlay. Implemented through the Gui fire rendering hook - the
 * fire texture is drawn compressed to the configured screen fraction.
 */
public class LowFireModule extends Module {
    private static LowFireModule instance;

    public final NumberSetting fireHeight = new NumberSetting("Fire Height", 0.35, 0.1, 1.0, 0.05, 2);

    public LowFireModule() {
        super("Low Fire", "Compresses the first-person fire overlay", Category.VISUALS);
        instance = this;
        addSettings(fireHeight);
    }

    public static LowFireModule get() {
        return instance;
    }

    public float getFireHeightFraction() {
        return isEnabled() ? fireHeight.getFloat() : 1.0f;
    }
}
