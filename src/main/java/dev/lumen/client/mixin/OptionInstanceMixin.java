package dev.lumen.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.lumen.client.modules.visual.FullbrightModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fullbright: smoothly boosts the gamma option's effective value without
 * mutating the user's actual brightness setting.
 */
@Mixin(OptionInstance.class)
public class OptionInstanceMixin<T> {

    @ModifyReturnValue(method = "get", at = @At("RETURN"))
    private T lumen$fullbright(T original) {
        if (!(original instanceof Double value)) return original;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return original;
        if ((Object) this != mc.options.gamma()) return original;

        FullbrightModule module = FullbrightModule.get();
        if (module == null || !module.isEnabled()) return original;
        float boost = module.getGammaBoost();
        if (boost <= 0.001f) return original;

        double boosted = value + boost * (16.0 - value);
        @SuppressWarnings("unchecked")
        T result = (T) (Double) Math.min(16.0, Math.max(value, boosted));
        return result;
    }
}
