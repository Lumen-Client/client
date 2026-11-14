package dev.lumen.client.core.setting;

public class KeybindSetting extends Setting {
    private int keyCode;
    private final int defaultKeyCode;

    public KeybindSetting(String name, int defaultKeyCode) {
        super(name, "");
        this.keyCode = defaultKeyCode;
        this.defaultKeyCode = defaultKeyCode;
    }

    public int get() {
        return keyCode;
    }

    public void set(int keyCode) {
        this.keyCode = keyCode;
    }

    public boolean isUnbound() {
        return keyCode == -1;
    }

    @Override
    public void reset() {
        keyCode = defaultKeyCode;
    }
}
