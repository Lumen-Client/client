package dev.lumen.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.lumen.client.modules.visual.TimeChangerModule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Client-side sky time override. Only touches ClientLevel instances, so the
 * integrated server's world time is never modified.
 */
@Mixin(Level.class)
public class LevelMixin {

    @ModifyReturnValue(method = "getDayTime", at = @At("RETURN"))
    private long lumen$overrideTime(long original) {
        if (!((Object) this instanceof ClientLevel)) return original;
        TimeChangerModule module = TimeChangerModule.get();
        if (module == null) return original;
        return module.getOverriddenTime(original);
    }
}
