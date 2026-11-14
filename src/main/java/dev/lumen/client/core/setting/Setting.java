package dev.lumen.client.core.setting;

import java.util.function.BooleanSupplier;

public abstract class Setting {
    private final String name;
    private final String description;
    private BooleanSupplier visibility = () -> true;

    protected Setting(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Setting setVisibleWhen(BooleanSupplier supplier) {
        this.visibility = supplier;
        return this;
    }

    public boolean isVisible() {
        return visibility.getAsBoolean();
    }

    public abstract void reset();
}
