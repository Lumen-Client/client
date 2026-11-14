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

    protected HudModule(String name, String description) {
        this(name, description, 6.0f, 6.0f);
    }

    protected HudModule(String name, String description, float defaultX, float defaultY) {
        super(name, description, Category.HUD);
        this.x = defaultX;
        this.y = defaultY;
        addSettings(scale, backgroundOpacity, roundedCorners);
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

    public abstract float getHudWidth();

    public abstract float getHudHeight();

    /**
     * Called every frame before the emptiness check so modules can update
     * animations and visibility flags even while hidden - without this,
     * elements that only become non-empty inside renderHud could never
     * appear (the dispatcher would keep skipping their render).
     */
    public void preRender() {
    }

    public abstract void renderHud(GuiGraphics graphics, float delta);

    public boolean shouldShowInEditor() {
        return true;
    }

    /**
     * When true the dispatcher suppresses the shared background, shadow and
     * border so empty HUD elements (no armor, no effects) vanish completely.
     */
    public boolean isContentEmpty() {
        return false;
    }
}
