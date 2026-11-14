package dev.lumen.client.render;

import dev.lumen.client.LumenClient;
import dev.lumen.client.anim.AnimationUtils;
import dev.lumen.client.core.HudModule;
import dev.lumen.client.render.RenderUtils;
import dev.lumen.client.theme.ColorUtils;
import dev.lumen.client.theme.ThemeManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

/**
 * Dispatches rendering for every enabled HUD module, plus the shared
 * per-frame theme animation update.
 */
public class HudRenderDispatcher implements HudElement {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("lumen", "hud_elements");
    public static final HudRenderDispatcher INSTANCE = new HudRenderDispatcher();

    private long lastNanos;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        LumenClient lumen = LumenClient.get();
        if (lumen == null) return;
        lumen.renderTick();

        Minecraft minecraft = Minecraft.getInstance();
        lumen.getModuleManager().get(dev.lumen.client.modules.hud.FpsModule.class).recordFrame();

        if (minecraft.options.hideGui || minecraft.player == null) return;
        if (minecraft.screen instanceof dev.lumen.client.gui.HudEditorScreen) return;

        float delta = AnimationUtils.deltaSeconds(lastNanos);
        lastNanos = System.nanoTime();

        ThemeManager theme = lumen.getThemeManager();

        for (HudModule module : lumen.getModuleManager().getHudModules()) {
            if (!module.isEnabled() || !module.shouldShowInEditor()) continue;
            module.preRender();
            boolean empty = module.isContentEmpty();
            graphics.pose().pushMatrix();
            graphics.pose().translate(module.getX(), module.getY());
            float scale = module.getScale();
            if (scale != 1.0f) {
                graphics.pose().scale(scale, scale);
            }
            float opacity = module.getBackgroundOpacity();
            if (opacity > 0.01f && !empty) {
                float width = module.getHudWidth();
                float height = module.getHudHeight();
                int bgColor = ColorUtils.withAlpha(theme.getBackground(), (int) (opacity * 255.0f));
                if (module.hasRoundedCorners()) {
                    RenderUtils.roundedRect(graphics, 0.0f, 0.0f, width, height, 5.0f, bgColor);
                } else {
                    RenderUtils.fill(graphics, 0.0f, 0.0f, width, height, bgColor);
                }
            }
            module.renderHud(graphics, delta);
            graphics.pose().popMatrix();
        }
    }
}
