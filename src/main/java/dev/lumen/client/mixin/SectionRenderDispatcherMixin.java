package dev.lumen.client.mixin;

import dev.lumen.client.modules.perf.ChunkOptimizerModule;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Frame-budgeted chunk section uploads and compile-queue capping.
 */
@Mixin(SectionRenderDispatcher.class)
public class SectionRenderDispatcherMixin {
    @Unique
    private long lumen$uploadStartNanos;

    @Inject(method = "uploadAllPendingUploads", at = @At("HEAD"), cancellable = true)
    private void lumen$budgetUploads(CallbackInfo ci) {
        ChunkOptimizerModule module = ChunkOptimizerModule.get();
        if (module == null || module.hasUploadBudget()) {
            lumen$uploadStartNanos = System.nanoTime();
        } else {
            ci.cancel();
        }
    }

    @Inject(method = "uploadAllPendingUploads", at = @At("RETURN"))
    private void lumen$spend(CallbackInfo ci) {
        ChunkOptimizerModule module = ChunkOptimizerModule.get();
        if (module != null && lumen$uploadStartNanos != 0) {
            module.onUploadSpent(System.nanoTime() - lumen$uploadStartNanos);
            lumen$uploadStartNanos = 0;
        }
    }

    @Inject(method = "schedule", at = @At("HEAD"), cancellable = true)
    private void lumen$queueCap(SectionRenderDispatcher.RenderSection.CompileTask task, CallbackInfo ci) {
        ChunkOptimizerModule module = ChunkOptimizerModule.get();
        if (module == null || !module.isEnabled()) return;
        if (((SectionRenderDispatcher) (Object) this).getCompileQueueSize() >= module.getMaxCompileQueue()) {
            ci.cancel();
        }
    }
}
