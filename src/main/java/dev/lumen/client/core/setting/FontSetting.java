package dev.lumen.client.core.setting;

import dev.lumen.client.font.FontManager;

public class FontSetting extends Setting {
    private String fontId;
    private final String defaultFontId;

    public FontSetting(String name, String defaultFontId) {
        super(name, "");
        this.fontId = defaultFontId;
        this.defaultFontId = defaultFontId;
    }

    public String get() {
        return fontId;
    }

    public void set(String fontId) {
        this.fontId = fontId;
        FontManager.get().onFontSettingChanged();
    }

    @Override
    public void reset() {
        fontId = defaultFontId;
        FontManager.get().onFontSettingChanged();
    }
}
