package dev.lumen.client.modules.visual;

import dev.lumen.client.core.Category;
import dev.lumen.client.core.Module;
import dev.lumen.client.core.setting.BooleanSetting;
import dev.lumen.client.core.setting.ColorSetting;
import dev.lumen.client.render.HitboxRenderer;
import dev.lumen.client.theme.ThemeManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Themed entity hitbox wireframes rendered through HitboxRenderer during
 * the world render pass (independent of vanilla's F3+B debug entry).
 */
public class HitboxesModule extends Module {
    private static HitboxesModule instance;

    public final BooleanSetting onlyLiving = new BooleanSetting("Only Living Entities", false);
    public final ColorSetting wireframeColor = new ColorSetting("Wireframe Color", 0xFFFF2E93, true);

    public HitboxesModule() {
        super("Entity Hitboxes", "Themed wireframes around entity bounding boxes", Category.VISUALS);
        instance = this;
        addSettings(onlyLiving, wireframeColor);
    }

    public static HitboxesModule get() {
        return instance;
    }

    public int wireframeColorValue() {
        return wireframeColor.isSyncWithTheme() ? ThemeManager.get().getPrimary() : wireframeColor.getRaw();
    }

    public boolean shouldRenderFor(Entity entity) {
        if (!isEnabled()) return false;
        if (onlyLiving.get() && !(entity instanceof LivingEntity)) return false;
        return true;
    }

    @Override
    public void onEnable() {
        HitboxRenderer.register();
    }
}
