package dev.lumen.client.mixin;

import dev.lumen.client.modules.visual.LowFireModule;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Compresses the first-person fire overlay to a configurable screen fraction.
 */
@Mixin(Gui.class)
public class GuiFireMixin {

    @Inject(method = "renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/Identifier;F)V",
            at = @At("HEAD"), cancellable = true)
    private void lumen$lowFire(GuiGraphics graphics, Identifier identifier, float alpha, CallbackInfo ci) {
        LowFireModule module = LowFireModule.get();
        if (module == null || !module.isEnabled()) return;
        if (identifier == null || !identifier.getPath().contains("fire")) return;

        int height = graphics.guiHeight();
        int lowHeight = Math.max(1, (int) (height * module.getFireHeightFraction()));
        graphics.blit(RenderPipelines.GUI_TEXTURED, identifier, 0, height - lowHeight,
                0.0f, 0.0f, graphics.guiWidth(), lowHeight, 256, 256);
        ci.cancel();
    }
}
