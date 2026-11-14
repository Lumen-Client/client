package dev.lumen.client.modules.hud;

import dev.lumen.client.anim.Animation;
import dev.lumen.client.anim.Easing;
import dev.lumen.client.core.HudModule;
import dev.lumen.client.render.RenderUtils;
import dev.lumen.client.theme.ColorUtils;
import dev.lumen.client.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.resources.Identifier;

/**
 * Live player face preview rendered from the local player's skin, gently
 * rotating via a bobbing highlight rather than a full 3D model (state-based
 * skin rendering keeps this zero-cost).
 */
public class MiniPlayerModule extends HudModule {
    private final Animation bobAnim = new Animation(2.4f, Easing::easeInOutQuart);
    private boolean rising = true;

    public MiniPlayerModule() {
        super("Mini Player", "Live local player preview");
    }

    @Override
    public void onClientTick() {
        rising = !rising;
        bobAnim.setTarget(rising);
    }

    @Override
    public float getHudWidth() {
        return 44.0f;
    }

    @Override
    public float getHudHeight() {
        return 44.0f;
    }

    @Override
    public void renderHud(GuiGraphics graphics, float delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ThemeManager theme = ThemeManager.get();
        bobAnim.update();
        float bob = bobAnim.getValue();

        RenderUtils.roundedRect(graphics, 0.0f, 0.0f, 44.0f, 44.0f, 6.0f,
                ColorUtils.withAlpha(theme.getCard(), 220));
        RenderUtils.border(graphics, 0.0f, 0.0f, 44.0f, 44.0f, 1.0f,
                theme.gradientColor(0.25f + bob * 0.5f));

        PlayerSkin skin = mc.player.getSkin();
        Identifier texture = skin.body().texturePath();
        graphics.pose().pushMatrix();
        graphics.pose().translate(10.0f, 10.0f);
        graphics.pose().scale(3.0f, 3.0f);
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture,
                0, 0, 8.0f, 8.0f, 8, 8, 64, 64, 0xFFFFFFFF);
        graphics.pose().popMatrix();
    }
}
