package dev.lumen.client.modules.qol;

import dev.lumen.client.core.Module;
import dev.lumen.client.core.Category;
import dev.lumen.client.core.setting.NumberSetting;
import dev.lumen.client.theme.ColorUtils;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

public class ChatStackingModule extends Module {
    public final NumberSetting windowSeconds = new NumberSetting("Window (s)", 10, 3, 60, 1, 0);

    private String lastText = "";
    private int repeatCount;
    private long lastMillis;

    public ChatStackingModule() {
        super("Chat Stacking", "Marks repeated messages with [x2], [x3]", Category.QOL);
        addSettings(windowSeconds);
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            if (!isEnabled() || overlay) return message;
            String text = message.getString();
            long now = System.currentTimeMillis();
            if (text.equals(lastText) && now - lastMillis < windowSeconds.getInt() * 1000L) {
                repeatCount++;
                Component badge = Component.literal("[x" + (repeatCount + 1) + "] ").withStyle(
                        s -> s.withColor(ColorUtils.withAlpha(0xFFFFD65E, 255) & 0xFFFFFF));
                return badge.copy().append(message);
            }
            lastText = text;
            repeatCount = 0;
            lastMillis = now;
            return message;
        });
    }
}
