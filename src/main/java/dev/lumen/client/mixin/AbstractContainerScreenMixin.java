package dev.lumen.client.mixin;

import dev.lumen.client.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws a small subtle Lumen logo in the bottom-left corner of every
 * container screen (inventory, chests, furnaces, crafting tables, ...).
 */
@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void lumen$containerLogo(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        int size = 10;
        int x = 4;
        int y = graphics.guiHeight() - size - 4;
        RenderUtils.drawTexture(graphics, RenderUtils.LOGO_TEXTURE, x, y, size, size, 0x90FFFFFF);
    }
}
