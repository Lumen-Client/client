package dev.lumen.client.modules.hud;

import dev.lumen.client.core.HudModule;
import dev.lumen.client.render.TextRenderer;
import dev.lumen.client.theme.ColorUtils;
import dev.lumen.client.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

public class PingModule extends HudModule {
    private int ping;
    private boolean local;
    private int refreshCounter;

    public PingModule() {
        super("Ping", "Latency to the current server", 6.0f, 44.0f);
    }

    public int getPing() {
        return ping;
    }

    private void refresh() {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || mc.player == null) {
            ping = -1;
            return;
        }
        if (mc.isSingleplayer()) {
            ping = 0;
            local = true;
            return;
        }
        local = false;
        PlayerInfo info = connection.getPlayerInfo(mc.player.getUUID());
        ping = info == null ? -1 : info.getLatency();
    }

    @Override
    public float getHudWidth() {
        return TextRenderer.width("Ping 000ms") + 10.0f;
    }

    @Override
    public float getHudHeight() {
        return TextRenderer.height() + 8.0f;
    }

    @Override
    public void renderHud(GuiGraphics graphics, float delta) {
        if (++refreshCounter >= 20) {
            refreshCounter = 0;
            refresh();
        }
        if (local) {
            TextRenderer.draw(graphics, "Ping Local", 5.0f, 4.0f, ColorUtils.withAlpha(ThemeManager.get().getText(), 200), true);
            return;
        }
        if (ping < 0) {
            TextRenderer.draw(graphics, "Ping --", 5.0f, 4.0f, ColorUtils.withAlpha(ThemeManager.get().getText(), 180), true);
            return;
        }
        int color = ping < 60 ? 0xFF5EFF8A : ping < 120 ? 0xFFFFD65E : 0xFFFF5E5E;
        TextRenderer.draw(graphics, "Ping " + ping + "ms", 5.0f, 4.0f, ColorUtils.withAlpha(color, 255), true);
    }
}
