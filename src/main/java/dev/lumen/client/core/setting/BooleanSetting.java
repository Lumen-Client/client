package dev.lumen.client.core.setting;

public class BooleanSetting extends Setting {
    private boolean value;
    private final boolean defaultValue;

    public BooleanSetting(String name, boolean defaultValue) {
        this(name, defaultValue, "");
    }

    public BooleanSetting(String name, boolean defaultValue, String description) {
        super(name, description);
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public boolean get() {
        return value;
    }

    public void set(boolean value) {
        this.value = value;
    }

    public void toggle() {
        this.value = !this.value;
    }

    @Override
    public void reset() {
        value = defaultValue;
    }
}
