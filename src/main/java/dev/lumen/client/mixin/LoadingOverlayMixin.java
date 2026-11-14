package dev.lumen.client.mixin;

import dev.lumen.client.render.RenderUtils;
import dev.lumen.client.render.TextRenderer;
import dev.lumen.client.theme.ColorUtils;
import dev.lumen.client.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




/**
 * Replaces the vanilla Mojang loading overlay with Lumen's branded loading
 * screen: client logo, rotating spinner, themed progress bar and live status
 * text. All vanilla reload/finish logic (tick, onFinish, fade timing) is
 * kept intact - only the visuals are replaced. On completion the logo
 * smoothly slides off the top of the screen.
 */
@Mixin(LoadingOverlay.class)
public class LoadingOverlayMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private ReloadInstance reload;

    @Shadow
    @Final
    private boolean fadeIn;

    @Shadow
    private float currentProgress;

    @Shadow
    private long fadeOutStart;

    @Shadow
    private long fadeInStart;

    @Shadow
    @Final
    public static long FADE_OUT_TIME;

    @Shadow
    @Final
    public static long FADE_IN_TIME;

    @Shadow
    private boolean isReadyToFadeOut() {
        throw new AssertionError();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void lumen$customLoadingScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ThemeManager theme = ThemeManager.get();
        long now = Util.getMillis();
        if (fadeIn && fadeInStart == -1L) {
            fadeInStart = now;
        }
        if (fadeOutStart == -1L) {
            currentProgress = Mth.clamp(currentProgress * 0.95f + reload.getActualProgress() * 0.05f, 0.0f, 1.0f);
        }

        float alpha = 1.0f;
        if (fadeIn && fadeInStart != -1L) {
            alpha = Mth.clamp((now - fadeInStart) / (float) FADE_IN_TIME, 0.0f, 1.0f);
        }
        float exit = 0.0f;
        if (fadeOutStart != -1L) {
            exit = Mth.clamp((now - fadeOutStart) / (float) FADE_OUT_TIME, 0.0f, 1.0f);
            alpha = 1.0f - exit;
        }
        if (alpha < 0.0f) alpha = 0.0f;

        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        RenderUtils.fill(graphics, 0, 0, w, h, theme.getBackground());

        float logoSize = Math.min(w, h) * 0.22f;
        float baseY = h * 0.38f;
        float slide = exit * exit * exit * (baseY + logoSize + 20.0f);
        float logoX = w / 2.0f - logoSize / 2.0f;
        float logoY = baseY - logoSize / 2.0f - slide;
        RenderUtils.drawTexture(graphics, RenderUtils.LOGO_TEXTURE_HI, logoX, logoY, logoSize, logoSize,
                ColorUtils.withAlpha(0xFFFFFF, (int) (255 * alpha)));

        float cx = w / 2.0f;
        float spinY = baseY + logoSize * 0.75f + 26.0f;
        drawSpinner(graphics, cx, spinY, 11.0f, now, alpha);

        if (exit <= 0.0f) {
            float barW = Math.min(w * 0.32f, 240.0f);
            float barY = spinY + 26.0f;
            RenderUtils.roundedRect(graphics, cx - barW / 2.0f, barY, barW, 6.0f, 3.0f,
                    ColorUtils.withAlpha(0xFF2A2A33, 255));
            RenderUtils.roundedGradient(graphics, cx - barW / 2.0f, barY,
                    Math.max(6.0f, barW * currentProgress), 6.0f, 3.0f,
                    theme.getPrimary(), theme.getSecondary(), false);

            String status = statusText();
            float statusW = TextRenderer.width(status);
            TextRenderer.draw(graphics, status, cx - statusW / 2.0f, barY + 14.0f,
                    ColorUtils.withAlpha(theme.getText(), (int) (210 * alpha)), true);

            String percent = Math.round(currentProgress * 100.0f) + "%";
            float percentW = TextRenderer.width(percent);
            TextRenderer.draw(graphics, percent, cx - percentW / 2.0f, barY - 18.0f,
                    ColorUtils.withAlpha(theme.getPrimary(), (int) (230 * alpha)), true);
        }

        if (fadeOutStart != -1L && now - fadeOutStart > FADE_OUT_TIME) {
            minecraft.setOverlay(null);
        }
        ci.cancel();
    }

    private String statusText() {
        if (fadeOutStart != -1L) {
            return "Entering world";
        }
        if (reload.isDone()) {
            return isReadyToFadeOut() ? "Finishing up" : "Building terrain";
        }
        return currentProgress < 0.05f ? "Preparing resources" : "Loading resources";
    }

    private static void drawSpinner(GuiGraphics graphics, float cx, float cy, float radius, long now, float alpha) {
        ThemeManager theme = ThemeManager.get();
        float phase = (now % 1100L) / 1100.0f * ((float) Math.PI * 2.0f);
        int active = 6;
        for (int k = 0; k < active; k++) {
            float segAlpha = (1.0f - k / (float) active) * alpha;
            if (segAlpha <= 0.02f) continue;
            int color = ColorUtils.withAlpha(k == 0 ? theme.getSecondary() : theme.getPrimary(),
                    (int) (255 * segAlpha));
            float a0 = phase - k * 0.42f;
            arc(graphics, cx, cy, radius, a0, a0 + 0.34f, color);
        }
    }

    private static void arc(GuiGraphics graphics, float cx, float cy, float radius, float a0, float a1, int color) {
        int steps = 6;
        float thickness = 2.5f;
        for (int i = 0; i <= steps; i++) {
            float angle = a0 + (a1 - a0) * i / steps;
            float px = cx + (float) Math.cos(angle) * radius - thickness / 2.0f;
            float py = cy + (float) Math.sin(angle) * radius - thickness / 2.0f;
            RenderUtils.fill(graphics, px, py, thickness, thickness, color);
        }
    }
}
