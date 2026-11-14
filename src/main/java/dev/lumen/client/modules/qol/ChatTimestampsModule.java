package dev.lumen.client.modules.qol;

import dev.lumen.client.core.Module;
import dev.lumen.client.core.Category;
import dev.lumen.client.core.setting.BooleanSetting;
import dev.lumen.client.core.setting.ColorSetting;
import dev.lumen.client.theme.ColorUtils;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatTimestampsModule extends Module {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public final BooleanSetting use24Hour = new BooleanSetting("24 Hour Clock", true);
    public final ColorSetting timestampColor = new ColorSetting("Timestamp Color", 0xFF9A9AA8);

    public ChatTimestampsModule() {
        super("Chat Timestamps", "Prefixes chat messages with the time", Category.QOL);
        addSettings(use24Hour, timestampColor);
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            if (!isEnabled() || overlay) return message;
            String time = LocalTime.now().format(FORMAT);
            String prefix = "[" + time + "] ";
            Component timestamp = Component.literal(prefix).withStyle(s -> s.withColor(timestampColor.get() & 0xFFFFFF));
            return timestamp.copy().append(message);
        });
    }
}
