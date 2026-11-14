package dev.lumen.client.mixin;

import dev.lumen.client.modules.visual.FreelookModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Redirects the local player's Entity#turn into the freelook camera while
 * active, keeping server-synced rotation untouched.
 */
@Mixin(Entity.class)
public class EntityTurnMixin {

    @Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true)
    private void lumen$freelookTurn(double yRot, double xRot, CallbackInfo ci) {
        FreelookModule freelook = FreelookModule.get();
        if (freelook == null || !freelook.isEnabled() || !freelook.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (Object) this == mc.player) {
            freelook.addLookDelta(yRot, xRot);
            ci.cancel();
        }
    }
}
