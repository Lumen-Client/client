package dev.lumen.client.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.lumen.client.gui.ClickGuiScreen;

/**
 * Mod Menu entry point: the "Configure" button opens Lumen's ClickGUI.
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ClickGuiScreen::new;
    }
}
