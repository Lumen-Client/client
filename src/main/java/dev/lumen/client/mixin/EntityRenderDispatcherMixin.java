package dev.lumen.client.mixin;

import dev.lumen.client.modules.perf.EntityOptimizerModule;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Distance culling for entity rendering.
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Shadow
    public Camera camera;

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void lumen$distanceCull(E entity, Frustum frustum, double x, double y, double z,
                                                       CallbackInfoReturnable<Boolean> cir) {
        EntityOptimizerModule module = EntityOptimizerModule.get();
        if (module == null || !module.isEnabled() || camera == null) return;
        Vec3 pos = camera.position();
        double dx = x - pos.x;
        double dy = y - pos.y;
        double dz = z - pos.z;
        if (module.shouldCullEntity(dx * dx + dy * dy + dz * dz)) {
            cir.setReturnValue(false);
        }
    }
}
