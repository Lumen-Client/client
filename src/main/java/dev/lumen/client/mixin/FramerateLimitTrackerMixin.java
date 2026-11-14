package dev.lumen.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.lumen.client.modules.perf.RenderOptimizerModule;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Unfocused/minimized FPS limiter.
 */
@Mixin(FramerateLimitTracker.class)
public class FramerateLimitTrackerMixin {

    @ModifyReturnValue(method = "getFramerateLimit", at = @At("RETURN"))
    private int lumen$unfocusedLimit(int original) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.isWindowActive()) return original;
        RenderOptimizerModule module = RenderOptimizerModule.get();
        if (module == null) return original;
        return module.getUnfocusedFpsLimit(original);
    }
}
