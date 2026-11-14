package dev.lumen.client.core.setting;

import dev.lumen.client.theme.ThemeManager;

public class ColorSetting extends Setting {
    private int value;
    private final int defaultValue;
    private boolean syncWithTheme;

    public ColorSetting(String name, int argb) {
        this(name, argb, false);
    }

    public ColorSetting(String name, int argb, boolean syncWithTheme) {
        super(name, "");
        this.value = argb;
        this.defaultValue = argb;
        this.syncWithTheme = syncWithTheme;
    }

    public int get() {
        return syncWithTheme ? ThemeManager.get().getPrimary() : value;
    }

    public int getRaw() {
        return value;
    }

    public void set(int argb) {
        this.value = argb;
    }

    public boolean isSyncWithTheme() {
        return syncWithTheme;
    }

    public void setSyncWithTheme(boolean sync) {
        this.syncWithTheme = sync;
    }

    @Override
    public void reset() {
        value = defaultValue;
    }
}
