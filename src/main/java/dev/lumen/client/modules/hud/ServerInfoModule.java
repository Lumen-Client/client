package dev.lumen.client.modules.hud;

import dev.lumen.client.core.HudModule;
import dev.lumen.client.render.TextRenderer;
import dev.lumen.client.theme.ColorUtils;
import dev.lumen.client.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ServerData;

public class ServerInfoModule extends HudModule {
    private long lastGameTime;
    private long lastRealNanos;
    private float estimatedTps = 20.0f;

    public ServerInfoModule() {
        super("Server Info", "Server address and estimated TPS");
    }

    @Override
    public void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = System.nanoTime();
        long gameTime = mc.level.getGameTime();
        if (lastRealNanos != 0 && gameTime != lastGameTime) {
            float ticks = gameTime - lastGameTime;
            float seconds = (now - lastRealNanos) / 1_000_000_000.0f;
            float tps = ticks / Math.max(0.05f, seconds);
            estimatedTps = estimatedTps * 0.8f + Math.min(20.0f, tps) * 0.2f;
        }
        lastGameTime = gameTime;
        lastRealNanos = now;
    }

    @Override
    public float getHudWidth() {
        String line = buildLine();
        return Math.max(TextRenderer.width(line), 90.0f) + 10.0f;
    }

    private String buildLine() {
        Minecraft mc = Minecraft.getInstance();
        ServerData data = mc.getCurrentServer();
        String address = data == null ? "Singleplayer" : data.ip;
        return address + " | " + String.format("%.1f", estimatedTps) + " TPS";
    }

    @Override
    public float getHudHeight() {
        return TextRenderer.height() + 8.0f;
    }

    @Override
    public void renderHud(GuiGraphics graphics, float delta) {
        int color = estimatedTps > 19.0f ? 0xFF5EFF8A : estimatedTps > 15.0f ? 0xFFFFD65E : 0xFFFF5E5E;
        TextRenderer.draw(graphics, buildLine(), 5.0f, 4.0f, ColorUtils.withAlpha(color, 255), true);
    }
}
