package dev.lumen.client.modules.visual;

import dev.lumen.client.core.Module;
import dev.lumen.client.core.Category;
import dev.lumen.client.core.setting.BooleanSetting;
import dev.lumen.client.core.setting.EnumSetting;
import dev.lumen.client.core.setting.NumberSetting;

/**
 * Client-side time override. The ClientLevel mixin redirects the day time the
 * sky renderer sees; the actual server time is untouched.
 */
public class TimeChangerModule extends Module {
    public enum Preset { DAY, SUNSET, NIGHT, CUSTOM }

    private static TimeChangerModule instance;

    public final EnumSetting<Preset> preset = new EnumSetting<>("Preset", Preset.class, Preset.DAY);
    public final NumberSetting customTime = new NumberSetting("Custom Time", 6000, 0, 24000, 100, 0);

    public TimeChangerModule() {
        super("Time Changer", "Client-side sky time override", Category.VISUALS);
        instance = this;
        addSettings(preset, customTime);
    }

    public static TimeChangerModule get() {
        return instance;
    }

    public long getOverriddenTime(long actual) {
        if (!isEnabled()) return actual;
        return switch (preset.get()) {
            case DAY -> 6000L;
            case SUNSET -> 12500L;
            case NIGHT -> 18000L;
            case CUSTOM -> customTime.getInt();
        };
    }
}
