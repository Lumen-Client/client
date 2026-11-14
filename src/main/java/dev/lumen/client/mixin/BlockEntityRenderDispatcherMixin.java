package dev.lumen.client.mixin;

import dev.lumen.client.modules.perf.EntityOptimizerModule;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Distance culling for block entities (chests, signs, item frames, ...).
 */
@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
    @Shadow
    private Vec3 cameraPos;

    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity, S extends BlockEntityRenderState> void lumen$distanceCull(
            E blockEntity, float partialTick, CrumblingOverlay crumblingOverlay,
            CallbackInfoReturnable<S> cir) {
        EntityOptimizerModule module = EntityOptimizerModule.get();
        if (module == null || !module.isEnabled() || cameraPos == null) return;
        BlockPos pos = blockEntity.getBlockPos();
        double dx = pos.getX() + 0.5 - cameraPos.x;
        double dy = pos.getY() + 0.5 - cameraPos.y;
        double dz = pos.getZ() + 0.5 - cameraPos.z;
        if (module.shouldCullBlockEntity(dx * dx + dy * dy + dz * dz)) {
            cir.setReturnValue(null);
        }
    }
}
