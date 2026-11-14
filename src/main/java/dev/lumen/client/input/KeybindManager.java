package dev.lumen.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.lumen.client.LumenClient;
import dev.lumen.client.core.Module;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * Registers all Lumen keybinds into Minecraft's native Controls menu under
 * the "Lumen Client" category, and keeps module keybinds in sync.
 */
public class KeybindManager {
    public static final KeyMapping.Category LUMEN_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("lumen", "lumen"));

    private final KeyMapping openClickGui = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.lumen.clickgui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, LUMEN_CATEGORY));
    private final KeyMapping openHudEditor = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.lumen.hud_editor", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, LUMEN_CATEGORY));
    private final KeyMapping zoom = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.lumen.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, LUMEN_CATEGORY));
    private final KeyMapping freelook = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.lumen.freelook", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, LUMEN_CATEGORY));
    private final KeyMapping fullbright = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.lumen.fullbright", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, LUMEN_CATEGORY));

    private final Map<Integer, Boolean> moduleKeyStates = new HashMap<>();

    public void tick(Minecraft minecraft) {
        while (openClickGui.consumeClick()) {
            LumenClient.get().openClickGui(minecraft);
        }
        while (openHudEditor.consumeClick()) {
            LumenClient.get().openHudEditor(minecraft);
        }
        while (fullbright.consumeClick()) {
            Module fullbrightModule = LumenClient.get().getModuleManager().get(dev.lumen.client.modules.visual.FullbrightModule.class);
            if (fullbrightModule != null) {
                fullbrightModule.toggle();
            }
        }

        if (minecraft.screen == null && minecraft.player != null) {
            for (Module module : LumenClient.get().getModuleManager().getModules()) {
                int key = module.getKeyCode();
                if (key == -1) continue;
                boolean down = InputConstants.isKeyDown(minecraft.getWindow(), key);
                Boolean previous = moduleKeyStates.get(key);
                if (down && !Boolean.TRUE.equals(previous)) {
                    module.handleKeyPress(key);
                }
                moduleKeyStates.put(key, down);
            }
        }
    }

    public boolean isZoomDown() {
        return zoom.isDown();
    }

    public boolean consumeZoomClick() {
        return zoom.consumeClick();
    }

    public boolean isFreelookDown() {
        return freelook.isDown();
    }

    public boolean consumeFreelookClick() {
        return freelook.consumeClick();
    }

    public KeyMapping getZoomMapping() {
        return zoom;
    }

    public KeyMapping getFreelookMapping() {
        return freelook;
    }

    public KeyMapping getFullbrightMapping() {
        return fullbright;
    }

    public KeyMapping getClickGuiMapping() {
        return openClickGui;
    }

    public KeyMapping getHudEditorMapping() {
        return openHudEditor;
    }
}
