package dev.lumen.client.mixin;

import dev.lumen.client.modules.visual.FreelookModule;
import dev.lumen.client.modules.visual.ZoomModule;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freelook mouse-delta redirection plus zoom dynamic sensitivity and
 * scroll-wheel magnification.
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Shadow
    private double accumulatedDX;
    @Shadow
    private double accumulatedDY;

    @Inject(method = "turnPlayer(D)V", at = @At("HEAD"), cancellable = true)
    private void lumen$decoupleLook(double multiplier, CallbackInfo ci) {
        FreelookModule freelook = FreelookModule.get();
        if (freelook != null && freelook.isEnabled() && freelook.isActive()) {
            freelook.addLookDelta(accumulatedDX, accumulatedDY);
            accumulatedDX = 0.0;
            accumulatedDY = 0.0;
            ci.cancel();
            return;
        }
        ZoomModule zoom = ZoomModule.get();
        if (zoom != null && zoom.isEnabled() && zoom.isZooming()) {
            float factor = (float) (1.0 / Math.sqrt(zoom.getZoomFactor()));
            accumulatedDX *= factor;
            accumulatedDY *= factor;
        }
    }

    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void lumen$zoomScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        ZoomModule zoom = ZoomModule.get();
        if (zoom != null && zoom.isEnabled() && zoom.isZooming() && Math.abs(yOffset) > 0.01) {
            zoom.scrollMagnification(Math.signum(yOffset));
            ci.cancel();
        }
    }
}
