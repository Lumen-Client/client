package dev.lumen.client.theme;

import dev.lumen.client.LumenClient;
import dev.lumen.client.anim.Animation;
import dev.lumen.client.anim.Easing;

import java.util.ArrayList;
import java.util.List;

/**
 * Single source of truth for every color the client draws. All GUI controls,
 * HUD elements, accents and wireframes bind to this manager so a single change
 * re-tints the entire client live.
 */
public class ThemeManager {
    private static ThemeManager instance;

    public enum GradientMode {
        SOLID, LINEAR, ANIMATED
    }

    private Theme theme = Theme.lumenPinkAndBlack();
    private final List<Theme> presets = new ArrayList<>();
    private final List<Theme> customPresets = new ArrayList<>();

    private final Animation flowAnimation = new Animation(1.0f, Easing::linear);
    private float flowPhase;

    public ThemeManager() {
        instance = this;
        presets.add(Theme.lumenPinkAndBlack());
        presets.add(Theme.midnightRose());
        presets.add(Theme.cyberMagenta());
        presets.add(Theme.obsidianWhite());
        presets.add(Theme.lavenderDusk());
    }

    public static ThemeManager get() {
        return LumenClient.get().getThemeManager();
    }

    public void renderTick(float deltaSeconds) {
        flowAnimation.setTarget(true);
        flowAnimation.update();
        flowPhase += deltaSeconds * theme.getGradientSpeed() * 0.25f;
        if (flowPhase > 1.0f) flowPhase -= 1.0f;
    }

    public int getPrimary() {
        return theme.getPrimaryColor();
    }

    public int getSecondary() {
        return theme.getSecondaryColor();
    }

    public int getBackground() {
        return theme.getBackgroundColor();
    }

    public int getCard() {
        return theme.getCardColor();
    }

    public int getText() {
        return theme.getTextColor();
    }

    public GradientMode getGradientMode() {
        try {
            return GradientMode.valueOf(theme.getGradientMode());
        } catch (IllegalArgumentException e) {
            return GradientMode.ANIMATED;
        }
    }

    public void setGradientMode(GradientMode mode) {
        theme.setGradientMode(mode.name());
        LumenClient.get().getConfigManager().markDirty();
    }

    public float getGradientSpeed() {
        return theme.getGradientSpeed();
    }

    public void setGradientSpeed(float speed) {
        theme.setGradientSpeed(speed);
        LumenClient.get().getConfigManager().markDirty();
    }

    /**
     * Resolves the gradient start color at a normalized position and time,
     * honoring the current gradient mode. Allocation free.
     */
    public int gradientColor(float position, float alphaScale) {
        int primary = theme.getPrimaryColor();
        int secondary = theme.getSecondaryColor();
        GradientMode mode = getGradientMode();
        float t = position;
        if (mode == GradientMode.ANIMATED) {
            t = position + flowPhase;
            t -= Math.floor(t);
        } else if (mode == GradientMode.SOLID) {
            return ColorUtils.scaleAlpha(primary, alphaScale);
        }
        return ColorUtils.scaleAlpha(ColorUtils.lerp(primary, secondary, t), alphaScale);
    }

    public int gradientColor(float position) {
        return gradientColor(position, 1.0f);
    }

    public Theme getTheme() {
        return theme;
    }

    public void applyTheme(Theme newTheme) {
        this.theme = newTheme;
        LumenClient.get().getConfigManager().markDirty();
    }

    public List<Theme> getPresets() {
        return presets;
    }

    public List<Theme> getCustomPresets() {
        return customPresets;
    }

    public void saveCurrentAsCustom(String ignoredName) {
        Theme copy = new Theme();
        copy.setPrimaryColor(theme.getPrimaryColor());
        copy.setSecondaryColor(theme.getSecondaryColor());
        copy.setBackgroundColor(theme.getBackgroundColor());
        copy.setCardColor(theme.getCardColor());
        copy.setTextColor(theme.getTextColor());
        copy.setGradientMode(theme.getGradientMode());
        copy.setGradientSpeed(theme.getGradientSpeed());
        if (customPresets.size() < 12) {
            customPresets.add(copy);
        }
        LumenClient.get().getConfigManager().markDirty();
    }

    public void setThemeField(String field, int value) {
        switch (field) {
            case "primary" -> theme.setPrimaryColor(value);
            case "secondary" -> theme.setSecondaryColor(value);
            case "background" -> theme.setBackgroundColor(value);
            case "card" -> theme.setCardColor(value);
            case "text" -> theme.setTextColor(value);
        }
        LumenClient.get().getConfigManager().markDirty();
    }

    public int getThemeField(String field) {
        return switch (field) {
            case "primary" -> theme.getPrimaryColor();
            case "secondary" -> theme.getSecondaryColor();
            case "background" -> theme.getBackgroundColor();
            case "card" -> theme.getCardColor();
            case "text" -> theme.getTextColor();
            default -> 0xFFFFFFFF;
        };
    }

    public Theme getThemeObject() {
        return theme;
    }

    public void setThemeObject(Theme theme) {
        this.theme = theme == null ? Theme.lumenPinkAndBlack() : theme;
    }

    public List<Theme> getCustomPresetsList() {
        return customPresets;
    }
}
