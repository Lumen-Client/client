package dev.lumen.client.mixin;

import dev.lumen.client.Lumen;
import dev.lumen.client.render.RenderUtils;
import dev.lumen.client.render.TextRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the Lumen logo and version tag in the bottom-left corner of the
 * main menu.
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void lumen$titleLogo(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        int size = 16;
        int x = 6;
        int y = graphics.guiHeight() - size - 6;
        RenderUtils.drawTexture(graphics, RenderUtils.LOGO_TEXTURE, x, y, size, size, 0xFFFFFFFF);
        TextRenderer.draw(graphics, "Lumen Client v" + Lumen.VERSION, x + size + 5, y + 5, 0xFFF2F2F7, true);
    }
}
