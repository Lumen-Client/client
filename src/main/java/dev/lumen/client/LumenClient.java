package dev.lumen.client;

import dev.lumen.client.anim.AnimationUtils;
import dev.lumen.client.core.ModuleManager;
import dev.lumen.client.core.config.ConfigManager;
import dev.lumen.client.font.FontManager;
import dev.lumen.client.gui.ClickGuiScreen;
import dev.lumen.client.gui.HudEditorScreen;
import dev.lumen.client.input.KeybindManager;
import dev.lumen.client.render.HudRenderDispatcher;
import dev.lumen.client.theme.ThemeManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LumenClient implements ClientModInitializer {
    public static final String MOD_ID = "lumen";
    public static final Logger LOGGER = LoggerFactory.getLogger("Lumen Client");

    private static LumenClient instance;

    private ConfigManager configManager;
    private ThemeManager themeManager;
    private FontManager fontManager;
    private ModuleManager moduleManager;
    private KeybindManager keybindManager;

    private long lastFrameNanos;

    public static LumenClient get() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ThemeManager getThemeManager() {
        return themeManager;
    }

    public FontManager getFontManager() {
        return fontManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public KeybindManager getKeybindManager() {
        return keybindManager;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("Lumen Client {} initializing", Lumen.VERSION);

        configManager = new ConfigManager();
        themeManager = new ThemeManager();
        fontManager = new FontManager(configManager.getFontsDir());
        moduleManager = new ModuleManager();
        keybindManager = new KeybindManager();

        fontManager.loadFonts();
        configManager.load();

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                HudRenderDispatcher.ID,
                HudRenderDispatcher.INSTANCE
        );

        ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "Lumen Config Shutdown Hook"));
        LOGGER.info("Lumen Client initialized");
    }

    private void onEndClientTick(Minecraft minecraft) {
        keybindManager.tick(minecraft);
        moduleManager.tickAll();
        if (minecraft.screen == null) {
            configManager.saveIfDirty();
        }
    }

    public void renderTick() {
        float delta = AnimationUtils.deltaSeconds(lastFrameNanos);
        lastFrameNanos = System.nanoTime();
        themeManager.renderTick(delta);
    }

    public void openClickGui(Minecraft minecraft) {
        if (minecraft.screen instanceof ClickGuiScreen) return;
        minecraft.setScreen(new ClickGuiScreen(null));
    }

    public void openHudEditor(Minecraft minecraft) {
        if (minecraft.screen instanceof HudEditorScreen) return;
        minecraft.setScreen(new HudEditorScreen());
    }

    private void shutdown() {
        try {
            configManager.save();
        } catch (Exception ignored) {
        }
    }
}
