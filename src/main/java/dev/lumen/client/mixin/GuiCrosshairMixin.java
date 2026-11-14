package dev.lumen.client.mixin;

import dev.lumen.client.modules.visual.CrosshairModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla crosshair with Lumen's custom crosshair designer.
 */
@Mixin(Gui.class)
public class GuiCrosshairMixin {

    @Inject(method = "renderCrosshair(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("HEAD"), cancellable = true)
    private void lumen$customCrosshair(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        CrosshairModule module = CrosshairModule.get();
        if (module != null && module.isEnabled()) {
            module.renderCrosshair(graphics, graphics.guiWidth() / 2, graphics.guiHeight() / 2);
            ci.cancel();
        }
    }
}
