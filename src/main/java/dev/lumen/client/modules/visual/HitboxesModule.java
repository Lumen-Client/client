package dev.lumen.client.modules.visual;

import dev.lumen.client.core.Category;
import dev.lumen.client.core.Module;
import dev.lumen.client.core.setting.BooleanSetting;
import dev.lumen.client.core.setting.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.world.entity.Entity;

/**
 * Themed entity hitboxes. Drives vanilla's 1.21.11 debug hitbox entry
 * (F3+B equivalent) through the official DebugScreenEntryList API, keeping
 * wireframe rendering fully vanilla-compatible.
 */
public class HitboxesModule extends Module {
    private static HitboxesModule instance;

    public final BooleanSetting onlyHostiles = new BooleanSetting("Only Living Entities", false);
    public final ColorSetting wireframeColor = new ColorSetting("Wireframe Color", 0xFFFF2E93, true);

    private boolean restored = true;

    public HitboxesModule() {
        super("Entity Hitboxes", "Vanilla entity hitbox debug wireframes", Category.VISUALS);
        instance = this;
        addSettings(onlyHostiles, wireframeColor);
    }

    public static HitboxesModule get() {
        return instance;
    }

    public boolean shouldRenderFor(Entity entity) {
        if (!isEnabled()) return false;
        if (onlyHostiles.get() && !(entity instanceof net.minecraft.world.entity.LivingEntity)) return false;
        return true;
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.debugEntries != null) {
            mc.debugEntries.setStatus(DebugScreenEntries.ENTITY_HITBOXES, DebugScreenEntryStatus.ALWAYS_ON);
            restored = false;
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (!restored && mc != null && mc.debugEntries != null) {
            mc.debugEntries.setStatus(DebugScreenEntries.ENTITY_HITBOXES, DebugScreenEntryStatus.NEVER);
            restored = true;
        }
    }
}
