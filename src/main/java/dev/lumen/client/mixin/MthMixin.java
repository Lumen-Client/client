package dev.lumen.client.mixin;

import dev.lumen.client.modules.perf.SystemOptimizerModule;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Zero-allocation 65536-entry sine lookup table for Mth.sin/cos hot paths.
 */
@Mixin(Mth.class)
public class MthMixin {
    @Unique
    private static final int LUMEN$TABLE_SIZE = 65536;
    @Unique
    private static final float LUMEN$INDEX_SCALE = LUMEN$TABLE_SIZE / (float) (Math.PI * 2.0);
    @Unique
    private static volatile float[] lumen$sinTable;

    @Unique
    private static float[] lumen$table() {
        float[] table = lumen$sinTable;
        if (table == null) {
            table = new float[LUMEN$TABLE_SIZE];
            for (int i = 0; i < LUMEN$TABLE_SIZE; i++) {
                table[i] = (float) Math.sin((double) i * Math.PI * 2.0 / LUMEN$TABLE_SIZE);
            }
            lumen$sinTable = table;
        }
        return table;
    }

    @Inject(method = "sin(D)F", at = @At("HEAD"), cancellable = true)
    private static void lumen$fastSin(double value, CallbackInfoReturnable<Float> cir) {
        SystemOptimizerModule module = SystemOptimizerModule.get();
        if (module == null || !module.useFastTrig()) return;
        int index = (int) (value * LUMEN$INDEX_SCALE) & (LUMEN$TABLE_SIZE - 1);
        cir.setReturnValue(lumen$table()[index]);
    }

    @Inject(method = "cos(D)F", at = @At("HEAD"), cancellable = true)
    private static void lumen$fastCos(double value, CallbackInfoReturnable<Float> cir) {
        SystemOptimizerModule module = SystemOptimizerModule.get();
        if (module == null || !module.useFastTrig()) return;
        int index = ((int) (value * LUMEN$INDEX_SCALE) + (LUMEN$TABLE_SIZE >> 2)) & (LUMEN$TABLE_SIZE - 1);
        cir.setReturnValue(lumen$table()[index]);
    }
}
