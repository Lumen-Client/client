package dev.lumen.client.modules.visual;

import dev.lumen.client.core.Module;
import dev.lumen.client.core.Category;
import dev.lumen.client.core.setting.BooleanSetting;
import dev.lumen.client.core.setting.ColorSetting;
import dev.lumen.client.core.setting.EnumSetting;
import dev.lumen.client.core.setting.NumberSetting;
import dev.lumen.client.render.RenderUtils;
import dev.lumen.client.theme.ColorUtils;
import dev.lumen.client.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class CrosshairModule extends Module {
    public enum Shape { CROSS, DOT, CIRCLE, CHEVRON, CROSS_DOT }

    private static CrosshairModule instance;

    public final EnumSetting<Shape> shape = new EnumSetting<>("Shape", Shape.class, Shape.CROSS);
    public final NumberSetting lineLength = new NumberSetting("Line Length", 6.0, 2.0, 16.0, 1.0, 0);
    public final NumberSetting thickness = new NumberSetting("Thickness", 2.0, 1.0, 5.0, 1.0, 0);
    public final NumberSetting gap = new NumberSetting("Center Gap", 3.0, 0.0, 10.0, 1.0, 0);
    public final NumberSetting outline = new NumberSetting("Outline", 1.0, 0.0, 3.0, 1.0, 0);
    public final NumberSetting outlineOpacity = new NumberSetting("Outline Opacity", 0.8, 0.0, 1.0, 0.05, 2);
    public final BooleanSetting topLine = new BooleanSetting("Top Line (T-Shape)", false);
    public final BooleanSetting syncTheme = new BooleanSetting("Sync With Theme", true);
    public final ColorSetting customColor = new ColorSetting("Custom Color", 0xFFFFFFFF);
    public final BooleanSetting dynamicSpread = new BooleanSetting("Dynamic Spread", true);
    public final BooleanSetting targetHighlight = new BooleanSetting("Target Highlight", true);

    private final dev.lumen.client.anim.Animation spreadAnim = new dev.lumen.client.anim.Animation(0.15f, dev.lumen.client.anim.Easing::easeOutCubic);
    private long previewNanos = System.nanoTime();
    private float previewPhase;
    private final dev.lumen.client.anim.Animation targetAnim = new dev.lumen.client.anim.Animation(0.15f, dev.lumen.client.anim.Easing::easeOutCubic);

    public CrosshairModule() {
        super("Custom Crosshair", "Fully customizable crosshair with live preview", Category.VISUALS);
        instance = this;
        addSettings(shape, lineLength, thickness, gap, outline, outlineOpacity, topLine,
                syncTheme, customColor, dynamicSpread, targetHighlight);
    }

    public static CrosshairModule get() {
        return instance;
    }

    public int resolveColor() {
        int base = syncTheme.get() ? ThemeManager.get().getPrimary() : customColor.get();
        if (targetHighlight.get()) {
            float t = targetAnim.getValue();
            if (t > 0.01f) {
                return ColorUtils.lerp(base, 0xFFFF4646, t);
            }
        }
        return base;
    }

    public float currentSpread() {
        float spread = gap.getFloat();
        if (dynamicSpread.get()) {
            spread += spreadAnim.getValue() * 4.0f;
        }
        return spread;
    }

    @Override
    public void onRenderTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (dynamicSpread.get()) {
            boolean moving = mc.player.isSprinting() || !mc.player.onGround() || mc.player.getAttackAnim(1.0f) > 0.01f;
            spreadAnim.setTarget(moving);
            spreadAnim.update();
        }
        if (targetHighlight.get()) {
            Entity target = mc.crosshairPickEntity;
            targetAnim.setTarget(target instanceof LivingEntity && target.isAlive());
            targetAnim.update();
        }
    }

    /**
     * Animated preview used in the Appearance tab: simulates movement spread
     * and target highlighting so the preview shows what the crosshair will
     * actually look like in game.
     */
    public void renderPreview(GuiGraphics graphics, int centerX, int centerY) {
        float dt = dev.lumen.client.anim.AnimationUtils.deltaSeconds(previewNanos);
        previewNanos = System.nanoTime();
        previewPhase += dt;
        if (dynamicSpread.get()) {
            spreadAnim.setTarget((previewPhase % 2.4f) < 1.2f);
            spreadAnim.update();
        }
        if (targetHighlight.get()) {
            targetAnim.setTarget((previewPhase % 3.2f) < 1.6f);
            targetAnim.update();
        }
        renderCrosshair(graphics, centerX, centerY);
    }

    /**
     * Called from the Gui mixin in place of the vanilla crosshair.
     */
    public void renderCrosshair(GuiGraphics graphics, int centerX, int centerY) {
        int color = resolveColor();
        float spread = currentSpread();
        float len = lineLength.getFloat();
        float th = thickness.getFloat();
        float out = outline.getFloat();
        int outlineColor = ColorUtils.withAlpha(0xFF000000, (int) (outlineOpacity.getFloat() * 255.0f));

        graphics.pose().pushMatrix();
        float scale = 1.0f;
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(scale, scale);
        int cx = 0;
        int cy = 0;

        switch (shape.get()) {
            case DOT -> drawRect(graphics, cx, cy, th, th, out, outlineColor, color);
            case CROSS -> {
                drawRect(graphics, cx - spread - th, cy - th / 2, len, th, out, outlineColor, color);
                if (topLine.get()) drawRect(graphics, cx - spread - th, cy - th / 2 - len - spread, th, len, out, outlineColor, color);
                drawRect(graphics, cx + spread, cy - th / 2, len, th, out, outlineColor, color);
                drawRect(graphics, cx - th / 2, cy - spread - th, th, len, out, outlineColor, color);
                drawRect(graphics, cx - th / 2, cy + spread, th, len, out, outlineColor, color);
            }
            case CROSS_DOT -> {
                drawRect(graphics, cx - spread - th, cy - th / 2, len, th, out, outlineColor, color);
                drawRect(graphics, cx + spread, cy - th / 2, len, th, out, outlineColor, color);
                drawRect(graphics, cx - th / 2, cy + spread, th, len, out, outlineColor, color);
                drawRect(graphics, cx - th / 2, cy - spread - th, th, len, out, outlineColor, color);
                drawRect(graphics, cx - th / 2, cy - th / 2, th, th, out, outlineColor, color);
            }
            case CIRCLE -> {
                float radius = spread + len;
                int segments = 24;
                for (int i = 0; i < segments; i++) {
                    double a0 = (i / (double) segments) * Math.PI * 2;
                    double a1 = ((i + 1) / (double) segments) * Math.PI * 2;
                    int x0 = (int) Math.round(Math.cos(a0) * radius);
                    int y0 = (int) Math.round(Math.sin(a0) * radius);
                    int x1 = (int) Math.round(Math.cos(a1) * radius);
                    int y1 = (int) Math.round(Math.sin(a1) * radius);
                    drawLine(graphics, x0, y0, x1, y1, th, out, outlineColor, color);
                }
            }
            case CHEVRON -> {
                float size = len;
                drawLine(graphics, (int) (-size - spread), (int) (-size / 2), (int) -spread, 0, th, out, outlineColor, color);
                drawLine(graphics, (int) -spread, 0, (int) (-size - spread), (int) (size / 2), th, out, outlineColor, color);
                drawLine(graphics, (int) spread, 0, (int) (size + spread), (int) (-size / 2), th, out, outlineColor, color);
                drawLine(graphics, (int) (size + spread), (int) (size / 2), (int) spread, 0, th, out, outlineColor, color);
            }
        }
        graphics.pose().popMatrix();
    }

    private void drawRect(GuiGraphics graphics, float x, float y, float w, float h, float outline, int outlineColor, int color) {
        if (outline > 0.0f) {
            RenderUtils.fill(graphics, x - outline, y - outline, w + outline * 2, h + outline * 2, outlineColor);
        }
        RenderUtils.fill(graphics, x, y, w, h, color);
    }

    private void drawLine(GuiGraphics graphics, float x0, float y0, float x1, float y1, float th, float outline, int outlineColor, int color) {
        int steps = (int) Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i++) {
            float t = steps == 0 ? 0 : i / (float) steps;
            float px = x0 + (x1 - x0) * t;
            float py = y0 + (y1 - y0) * t;
            drawRect(graphics, px - th / 2, py - th / 2, th, th, outline, outlineColor, color);
        }
    }
}
