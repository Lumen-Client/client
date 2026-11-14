package dev.lumen.client.modules.hud;

import dev.lumen.client.core.HudModule;
import dev.lumen.client.render.TextRenderer;
import dev.lumen.client.theme.ColorUtils;
import dev.lumen.client.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class FpsModule extends HudModule {
    private static final int FRAME_SAMPLES = 600;

    private final long[] frameNanos = new long[FRAME_SAMPLES];
    private final long[] sortedScratch = new long[FRAME_SAMPLES];
    private int frameCursor;
    private int sampleCount;
    private long lastFrameTime;
    private int cachedFps;
    private int cachedOnePercentLow;
    private int recomputeCounter;

    public FpsModule() {
        super("FPS Counter", "Frames per second with 1% lows", 6.0f, 24.0f);
    }

    public void recordFrame() {
        long now = System.nanoTime();
        if (lastFrameTime != 0) {
            frameNanos[frameCursor] = now - lastFrameTime;
            frameCursor = (frameCursor + 1) % FRAME_SAMPLES;
            if (sampleCount < FRAME_SAMPLES) sampleCount++;
        }
        lastFrameTime = now;
    }

    public int getFps() {
        return cachedFps;
    }

    public int getOnePercentLow() {
        return cachedOnePercentLow;
    }

    private void recompute() {
        if (sampleCount < 10) {
            cachedFps = Minecraft.getInstance().getFps();
            cachedOnePercentLow = cachedFps;
            return;
        }
        int count = 0;
        double total = 0.0;
        for (int i = 0; i < sampleCount; i++) {
            long f = frameNanos[i];
            if (f > 0) {
                sortedScratch[count++] = f;
                total += f;
            }
        }
        if (count == 0) return;
        java.util.Arrays.sort(sortedScratch, 0, count);
        cachedFps = (int) Math.round(count / (total / 1_000_000_000.0));
        int worstCount = Math.max(1, count / 100);
        long sum = 0;
        for (int i = count - worstCount; i < count; i++) {
            sum += sortedScratch[i];
        }
        cachedOnePercentLow = (int) Math.round(worstCount / (sum / 1_000_000_000.0));
    }

    @Override
    public float getHudWidth() {
        return Math.max(TextRenderer.width("FPS 000 | 1% 000"), 74.0f) + 10.0f;
    }

    @Override
    public float getHudHeight() {
        return TextRenderer.height() + 8.0f;
    }

    @Override
    public void renderHud(GuiGraphics graphics, float delta) {
        if (++recomputeCounter >= 20) {
            recomputeCounter = 0;
            recompute();
        }
        ThemeManager theme = ThemeManager.get();
        String text = "FPS " + cachedFps + " | 1% " + cachedOnePercentLow;
        int color = cachedFps >= 120 ? theme.getPrimary() : cachedFps >= 60 ? 0xFFB8FF5E : 0xFFFF5E5E;
        TextRenderer.draw(graphics, text, 5.0f, 4.0f, ColorUtils.withAlpha(color, 255), true);
    }
}
