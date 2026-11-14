package dev.lumen.client.core.setting;

public class StringSetting extends Setting {
    private String value;
    private final String defaultValue;

    public StringSetting(String name, String defaultValue) {
        super(name, "");
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public String get() {
        return value;
    }

    public void set(String value) {
        if (value != null) {
            this.value = value;
        }
    }

    @Override
    public void reset() {
        value = defaultValue;
    }
}
