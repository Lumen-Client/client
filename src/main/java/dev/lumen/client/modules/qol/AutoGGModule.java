package dev.lumen.client.modules.qol;

import dev.lumen.client.LumenClient;
import dev.lumen.client.core.Module;
import dev.lumen.client.core.Category;
import dev.lumen.client.core.setting.NumberSetting;
import dev.lumen.client.core.setting.StringSetting;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;

public class AutoGGModule extends Module {
    private static final Pattern[] TRIGGERS = {
            Pattern.compile("(?i)\\b(?:gg|good game|victory|winner)\\b.*"),
            Pattern.compile("(?i).*\\bwon the (?:game|match|round)\\b.*")
    };

    public final StringSetting message = new StringSetting("Message", "gg");
    public final NumberSetting delayTicks = new NumberSetting("Delay (ticks)", 10, 0, 60, 1, 0);

    private final CopyOnWriteArraySet<String> recentMessages = new CopyOnWriteArraySet<>();
    private int pendingSend;
    private int cooldown;

    public AutoGGModule() {
        super("AutoGG", "Automatically says gg after a game ends", Category.QOL);
        addSettings(message, delayTicks);
        ClientReceiveMessageEvents.GAME.register(this::onChatMessage);
        ClientReceiveMessageEvents.CHAT.register(
                (component, signedMessage, sender, params, receptionTimestamp) -> onChatMessage(component, false));
    }

    private void onChatMessage(net.minecraft.network.chat.Component component, boolean overlay) {
        if (!isEnabled() || overlay || cooldown > 0 || pendingSend > 0) return;
        for (Pattern pattern : TRIGGERS) {
            if (pattern.matcher(component.getString()).matches()) {
                pendingSend = Math.max(1, delayTicks.getInt());
                cooldown = 200;
                break;
            }
        }
    }

    @Override
    public void onClientTick() {
        if (cooldown > 0) cooldown--;
        if (pendingSend > 0) {
            pendingSend--;
            if (pendingSend == 0) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.connection.sendChat(message.get());
                }
            }
        }
    }
}
