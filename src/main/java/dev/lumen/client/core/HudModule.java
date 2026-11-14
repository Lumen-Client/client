package dev.lumen.client.core;

import dev.lumen.client.LumenClient;
import dev.lumen.client.core.setting.BooleanSetting;
import dev.lumen.client.core.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphics;

public abstract class HudModule extends Module {
    protected float x = 6.0f;
    protected float y = 6.0f;
    protected final NumberSetting scale = new NumberSetting("Scale", 1.0, 0.5, 2.5, 0.05, 2);
    protected final NumberSetting backgroundOpacity = new NumberSetting("Background Opacity", 0.55, 0.0, 1.0, 0.05, 2);
    protected final BooleanSetting roundedCorners = new BooleanSetting("Rounded Corners", true);
    protected final BooleanSetting dropShadow = new BooleanSetting("Drop Shadow", true);

    protected HudModule(String name, String description) {
        super(name, description, Category.HUD);
        addSettings(scale, backgroundOpacity, roundedCorners, dropShadow);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getScale() {
        return scale.getFloat();
    }

    public void nudgeScale(float delta) {
        scale.set(scale.get() + delta);
        LumenClient.get().getConfigManager().markDirty();
    }

    public float getBackgroundOpacity() {
        return backgroundOpacity.getFloat();
    }

    public boolean hasRoundedCorners() {
        return roundedCorners.get();
    }

    public boolean hasDropShadow() {
        return dropShadow.get();
    }

    public abstract float getHudWidth();

    public abstract float getHudHeight();

    public abstract void renderHud(GuiGraphics graphics, float delta);

    public boolean shouldShowInEditor() {
        return true;
    }
}
