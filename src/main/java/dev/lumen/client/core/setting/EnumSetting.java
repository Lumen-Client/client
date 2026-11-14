package dev.lumen.client.core.setting;

public class EnumSetting<T extends Enum<T>> extends Setting {
    private final Class<T> enumClass;
    private T value;
    private final T defaultValue;

    public EnumSetting(String name, Class<T> enumClass, T defaultValue) {
        super(name, "");
        this.enumClass = enumClass;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    public void cycle() {
        T[] constants = enumClass.getEnumConstants();
        this.value = constants[(value.ordinal() + 1) % constants.length];
    }

    public T[] getConstants() {
        return enumClass.getEnumConstants();
    }

    @Override
    public void reset() {
        value = defaultValue;
    }
}
