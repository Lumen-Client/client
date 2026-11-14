package dev.lumen.client.core;

import dev.lumen.client.LumenClient;
import dev.lumen.client.core.setting.Setting;
import dev.lumen.client.input.KeybindManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting> settings = new ArrayList<>();
    private boolean enabled;
    private int keyCode = -1;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) {
            onEnable();
            dev.lumen.client.sfx.ClientSounds.moduleEnable();
        } else {
            onDisable();
            dev.lumen.client.sfx.ClientSounds.moduleDisable();
        }
        LumenClient.get().getConfigManager().markDirty();
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
        LumenClient.get().getConfigManager().markDirty();
    }

    public boolean isBound() {
        return keyCode != -1;
    }

    public void handleKeyPress(int keyCode) {
        if (keyCode == this.keyCode) {
            toggle();
        }
    }

    protected void addSettings(Setting... settingArray) {
        Collections.addAll(settings, settingArray);
    }

    public List<Setting> getSettings() {
        return settings;
    }

    public <T extends Setting> T getSetting(String settingName, Class<T> type) {
        for (Setting setting : settings) {
            if (setting.getName().equals(settingName) && type.isInstance(setting)) {
                return type.cast(setting);
            }
        }
        return null;
    }

    public void onEnable() {}

    public void onDisable() {}

    public void onClientTick() {}

    public void onWorldTick() {}

    public void onRenderTick() {}

    protected void registerKeybind(int defaultKey) {
        this.keyCode = defaultKey;
    }
}
