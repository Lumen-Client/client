package dev.lumen.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.lumen.client.modules.visual.LowShieldModule;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lowers the first-person shield while blocking. The hand quads are built
 * from this pose stack, so translating it moves the held shield down without
 * affecting the camera or other players.
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Inject(method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
            at = @At("HEAD"))
    private void lumen$lowShield(float partialTick, PoseStack poseStack, SubmitNodeCollector collector,
                                 LocalPlayer player, int light, CallbackInfo ci) {
        LowShieldModule module = LowShieldModule.get();
        if (module == null || !module.isEnabled()) return;
        if (player == null || !player.isUsingItem()) return;
        if (!(player.getUseItem().getItem() instanceof ShieldItem)) return;
        poseStack.translate(0.0f, -module.getOffset(), 0.0f);
    }
}
