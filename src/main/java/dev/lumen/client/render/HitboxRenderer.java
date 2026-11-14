package dev.lumen.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.lumen.client.modules.visual.HitboxesModule;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Renders themed entity hitbox wireframes during the world render pass via
 * Fabric's AFTER_ENTITIES event, camera-relative and allocation-light.
 */
public final class HitboxRenderer {
    private static boolean registered;

    private HitboxRenderer() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        WorldRenderEvents.AFTER_ENTITIES.register(HitboxRenderer::afterEntities);
    }

    private static void afterEntities(WorldRenderContext context) {
        HitboxesModule module = HitboxesModule.get();
        if (module == null || !module.isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        Vec3 cam = context.gameRenderer().getMainCamera().position();
        VertexConsumer lines = context.consumers().getBuffer(RenderTypes.lines());
        int argb = module.wireframeColorValue() | 0xFF000000;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!module.shouldRenderFor(entity)) continue;
            if (entity == mc.player) continue;
            AABB box = entity.getBoundingBox().move(-cam.x, -cam.y, -cam.z);
            drawBox(lines, box, argb);
        }
    }

    private static void drawBox(VertexConsumer vc, AABB box, int argb) {
        float x0 = (float) box.minX;
        float y0 = (float) box.minY;
        float z0 = (float) box.minZ;
        float x1 = (float) box.maxX;
        float y1 = (float) box.maxY;
        float z1 = (float) box.maxZ;
        line(vc, x0, y0, z0, x1, y0, z0, argb);
        line(vc, x1, y0, z0, x1, y0, z1, argb);
        line(vc, x1, y0, z1, x0, y0, z1, argb);
        line(vc, x0, y0, z1, x0, y0, z0, argb);
        line(vc, x0, y1, z0, x1, y1, z0, argb);
        line(vc, x1, y1, z0, x1, y1, z1, argb);
        line(vc, x1, y1, z1, x0, y1, z1, argb);
        line(vc, x0, y1, z1, x0, y1, z0, argb);
        line(vc, x0, y0, z0, x0, y1, z0, argb);
        line(vc, x1, y0, z0, x1, y1, z0, argb);
        line(vc, x1, y0, z1, x1, y1, z1, argb);
        line(vc, x0, y0, z1, x0, y1, z1, argb);
    }

    private static void line(VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
        float nx = x1 - x0;
        float ny = y1 - y0;
        float nz = z1 - z0;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0e-4f) return;
        nx /= len;
        ny /= len;
        nz /= len;
        vc.addVertex(x0, y0, z0).setColor(argb).setNormal(nx, ny, nz);
        vc.addVertex(x1, y1, z1).setColor(argb).setNormal(nx, ny, nz);
    }
}
