package dev.lumen.client.gui;

import dev.lumen.client.LumenClient;
import dev.lumen.client.anim.Animation;
import dev.lumen.client.anim.AnimationUtils;
import dev.lumen.client.anim.Easing;
import dev.lumen.client.anim.AnimationUtils.VelocityState;
import dev.lumen.client.core.HudModule;
import dev.lumen.client.render.RenderUtils;
import dev.lumen.client.render.TextRenderer;
import dev.lumen.client.theme.ColorUtils;
import dev.lumen.client.theme.ThemeManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * HUD Layout Editor: alignment grid, free dragging with magnetic center
 * snapping, accent-colored snap guides, scroll-to-scale and spring release.
 */
public class HudEditorScreen extends Screen {
    private final Screen parent;
    private final Animation gridAnim = new Animation(0.35f, Easing::easeOutCubic);
    private final Animation fadeAnim = new Animation(0.2f, Easing::easeOutCubic);
    private final Animation scaleAnim = new Animation(0.24f, Easing::easeOutBack);
    private final Animation guideAlpha = new Animation(0.15f, Easing::easeOutCubic);
    private boolean closing;

    private HudModule dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    private boolean snapCenterX;
    private boolean snapCenterY;

    private HudModule settling;
    private final VelocityState settleVelX = new VelocityState();
    private final VelocityState settleVelY = new VelocityState();

    private long lastNanos = System.nanoTime();

    public HudEditorScreen(Screen parent) {
        super(Component.literal("Lumen HUD Editor"));
        this.parent = parent;
    }

    public HudEditorScreen() {
        this(null);
    }

    @Override
    protected void init() {
        gridAnim.snap(false);
        gridAnim.setTarget(true);
        fadeAnim.snap(false);
        fadeAnim.setTarget(true);
        scaleAnim.snap(false);
        scaleAnim.setTarget(true);
    }

    private void startClosing() {
        closing = true;
        gridAnim.setTarget(false);
        fadeAnim.setTarget(false);
        scaleAnim.setTarget(false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        float dt = AnimationUtils.deltaSeconds(lastNanos);
        lastNanos = System.nanoTime();
        gridAnim.update();
        fadeAnim.update();
        scaleAnim.update();
        guideAlpha.update();
        ThemeManager.get().renderTick(dt);

        float alpha = fadeAnim.getValue();
        if (closing && alpha <= 0.01f) {
            LumenClient.get().getConfigManager().save();
            minecraft.setScreen(parent);
            return;
        }

        RenderUtils.fill(graphics, 0, 0, width, height, ColorUtils.withAlpha(0x55000000, (int) (255 * alpha)));

        ThemeManager theme = ThemeManager.get();
        float gridAlpha = gridAnim.getValue() * 0.30f * alpha;
        int gridColor = ColorUtils.withAlpha(0xFFFFFF, (int) (255 * gridAlpha));
        for (int gx = 0; gx < width; gx += 20) {
            RenderUtils.fill(graphics, gx, 0, 1, height, gridColor);
        }
        for (int gy = 0; gy < height; gy += 20) {
            RenderUtils.fill(graphics, 0, gy, width, 1, gridColor);
        }

        float scale = 0.92f + 0.08f * scaleAnim.getValue();
        float cx = width / 2.0f;
        float cy = height / 2.0f;
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-cx, -cy);

        List<HudModule> modules = LumenClient.get().getModuleManager().getHudModules();
        for (HudModule module : modules) {
            if (!module.isEnabled() || !module.shouldShowInEditor()) continue;
            renderModule(graphics, module, mouseX, mouseY, alpha);
        }

        float guides = guideAlpha.getValue() * alpha;
        if (guides > 0.01f) {
            int guideColor = ColorUtils.withAlpha(theme.getPrimary(), (int) (220 * guides));
            if (snapCenterX) {
                RenderUtils.fill(graphics, width / 2.0f - 0.5f, 0, 1, height, guideColor);
            }
            if (snapCenterY) {
                RenderUtils.fill(graphics, 0, height / 2.0f - 0.5f, width, 1, guideColor);
            }
        }

        graphics.pose().popMatrix();

        String hint = "Drag to move - Scroll to scale - Center axes snap - ESC to save & exit";
        float hintW = TextRenderer.width(hint);
        float hintY = height - 18.0f;
        RenderUtils.roundedRect(graphics, width / 2.0f - hintW / 2.0f - 8.0f, hintY - 4.0f, hintW + 16.0f, 15.0f, 5.0f,
                ColorUtils.withAlpha(theme.getBackground(), (int) (200 * alpha)));
        TextRenderer.draw(graphics, hint, width / 2.0f - hintW / 2.0f, hintY,
                ColorUtils.withAlpha(theme.getText(), (int) (190 * alpha)), true);

        TextRenderer.drawScaled(graphics, "HUD Layout Editor", 8.0f, 6.0f, theme.gradientColor(0.0f, alpha), 1.1f, true);
    }

    private void renderModule(GuiGraphics graphics, HudModule module, int mouseX, int mouseY, float alpha) {
        ThemeManager theme = ThemeManager.get();
        graphics.pose().pushMatrix();
        graphics.pose().translate(module.getX(), module.getY());
        float scale = module.getScale();
        if (scale != 1.0f) {
            graphics.pose().scale(scale, scale);
        }

        float w = module.getHudWidth();
        float h = module.getHudHeight();
        float opacity = Math.max(0.35f, module.getBackgroundOpacity());
        int bgColor = ColorUtils.withAlpha(theme.getBackground(), (int) (opacity * 255.0f));
        if (module.hasDropShadow()) {
            RenderUtils.dropShadow(graphics, 0.0f, 0.0f, w, h);
        }
        if (module.hasRoundedCorners()) {
            RenderUtils.roundedRect(graphics, 0.0f, 0.0f, w, h, 5.0f, bgColor);
        } else {
            RenderUtils.fill(graphics, 0.0f, 0.0f, w, h, bgColor);
        }
        module.renderHud(graphics, 0.016f);

        boolean hovered = isOverModule(module, mouseX, mouseY);
        int borderColor = module == dragging
                ? theme.gradientColor(0.5f, alpha)
                : ColorUtils.withAlpha(hovered ? theme.getPrimary() : theme.getCard(), (int) ((hovered ? 255 : 120) * alpha));
        float thickness = module == dragging ? 2.0f : 1.0f;
        RenderUtils.border(graphics, -1.0f, -1.0f, w + 2.0f, h + 2.0f, thickness, borderColor);

        graphics.pose().popMatrix();
    }

    private boolean isOverModule(HudModule module, double mx, double my) {
        float scale = module.getScale();
        float w = module.getHudWidth() * scale;
        float h = module.getHudHeight() * scale;
        return mx >= module.getX() && mx <= module.getX() + w && my >= module.getY() && my <= module.getY() + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == 0) {
            List<HudModule> modules = LumenClient.get().getModuleManager().getHudModules();
            for (int i = modules.size() - 1; i >= 0; i--) {
                HudModule module = modules.get(i);
                if (!module.isEnabled() || !module.shouldShowInEditor()) continue;
                if (isOverModule(module, event.x(), event.y())) {
                    dragging = module;
                    settling = null;
                    dragOffsetX = event.x() - module.getX();
                    dragOffsetY = event.y() - module.getY();
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging != null) {
            float newX = (float) (event.x() - dragOffsetX);
            float newY = (float) (event.y() - dragOffsetY);
            float w = dragging.getHudWidth() * dragging.getScale();
            float h = dragging.getHudHeight() * dragging.getScale();

            snapCenterX = Math.abs(newX + w / 2.0f - width / 2.0f) < 6.0f;
            snapCenterY = Math.abs(newY + h / 2.0f - height / 2.0f) < 6.0f;
            if (snapCenterX) newX = width / 2.0f - w / 2.0f;
            if (snapCenterY) newY = height / 2.0f - h / 2.0f;

            newX = Math.max(-w / 2.0f, Math.min(width - w / 2.0f, newX));
            newY = Math.max(-h / 2.0f, Math.min(height - h / 2.0f, newY));

            dragging.setPosition(newX, newY);
            guideAlpha.setTarget(snapCenterX || snapCenterY);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            settling = dragging;
            settleVelX.velocity = 0.0f;
            settleVelY.velocity = 0.0f;
            dragging = null;
            guideAlpha.setTarget(false);
            snapCenterX = false;
            snapCenterY = false;
            LumenClient.get().getConfigManager().markDirty();
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<HudModule> modules = LumenClient.get().getModuleManager().getHudModules();
        for (HudModule module : modules) {
            if (module.isEnabled() && module.shouldShowInEditor() && isOverModule(module, mouseX, mouseY)) {
                module.nudgeScale((float) scrollY * 0.05f);
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            startClosing();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void tick() {
        if (settling != null) {
            float delta = 0.05f;
            float nx = AnimationUtils.springApproach(settling.getX(), Math.round(settling.getX()), settleVelX, delta);
            float ny = AnimationUtils.springApproach(settling.getY(), Math.round(settling.getY()), settleVelY, delta);
            settling.setPosition(nx, ny);
            if (Math.abs(settleVelX.velocity) < 0.01f && Math.abs(settleVelY.velocity) < 0.01f) {
                settling.setPosition(Math.round(settling.getX()), Math.round(settling.getY()));
                settling = null;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
