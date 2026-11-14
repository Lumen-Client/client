package dev.lumen.client.modules.hud;

import dev.lumen.client.anim.Animation;
import dev.lumen.client.anim.Easing;
import dev.lumen.client.core.HudModule;
import dev.lumen.client.render.RenderUtils;
import dev.lumen.client.render.TextRenderer;
import dev.lumen.client.theme.ColorUtils;
import dev.lumen.client.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class KeystrokesModule extends HudModule {
    private final Animation wAnim = new Animation(0.12f, Easing::easeOutCubic);
    private final Animation aAnim = new Animation(0.12f, Easing::easeOutCubic);
    private final Animation sAnim = new Animation(0.12f, Easing::easeOutCubic);
    private final Animation dAnim = new Animation(0.12f, Easing::easeOutCubic);
    private final Animation lmbAnim = new Animation(0.12f, Easing::easeOutCubic);
    private final Animation rmbAnim = new Animation(0.12f, Easing::easeOutCubic);

    private int leftCps;
    private int rightCps;
    private long[] leftClicks = new long[100];
    private long[] rightClicks = new long[100];
    private int leftCursor;
    private int rightCursor;
    private boolean lastLeft;
    private boolean lastRight;

    public KeystrokesModule() {
        super("Keystrokes", "WASD, mouse buttons and CPS", 6.0f, 130.0f);
    }

    public void recordClick(boolean left) {
        long now = System.currentTimeMillis();
        if (left) {
            leftClicks[leftCursor++ % 100] = now;
        } else {
            rightClicks[rightCursor++ % 100] = now;
        }
    }

    private int countCps(long[] clicks, int cursor) {
        long cutoff = System.currentTimeMillis() - 1000;
        int count = 0;
        for (long t : clicks) {
            if (t > cutoff) count++;
        }
        return count;
    }

    @Override
    public void onClientTick() {
        leftCps = countCps(leftClicks, leftCursor);
        rightCps = countCps(rightClicks, rightCursor);
    }

    @Override
    public float getHudWidth() {
        return 96.0f;
    }

    @Override
    public float getHudHeight() {
        return 96.0f;
    }

    @Override
    public void renderHud(GuiGraphics graphics, float delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ThemeManager theme = ThemeManager.get();

        wAnim.setTarget(mc.options.keyUp.isDown());
        aAnim.setTarget(mc.options.keyLeft.isDown());
        sAnim.setTarget(mc.options.keyDown.isDown());
        dAnim.setTarget(mc.options.keyRight.isDown());
        boolean leftDown = mc.mouseHandler.isLeftPressed();
        boolean rightDown = mc.mouseHandler.isRightPressed();
        if (leftDown && !lastLeft) recordClick(true);
        if (rightDown && !lastRight) recordClick(false);
        lastLeft = leftDown;
        lastRight = rightDown;
        lmbAnim.setTarget(leftDown);
        rmbAnim.setTarget(rightDown);

        wAnim.update();
        aAnim.update();
        sAnim.update();
        dAnim.update();
        lmbAnim.update();
        rmbAnim.update();

        float size = 28.0f;
        float gap = 4.0f;
        float row2Y = size + gap;
        float row3Y = row2Y + size + gap;

        drawKey(graphics, "W", size + gap, 0.0f, size, wAnim, theme);
        drawKey(graphics, "A", 0.0f, row2Y, size, aAnim, theme);
        drawKey(graphics, "S", size + gap, row2Y, size, sAnim, theme);
        drawKey(graphics, "D", (size + gap) * 2, row2Y, size, dAnim, theme);
        drawMouseButton(graphics, "LMB | " + leftCps, 0.0f, row3Y, (size + gap) * 2, lmbAnim, theme);
        drawMouseButton(graphics, "RMB | " + rightCps, 0.0f, row3Y + size + gap, (size + gap) * 2, rmbAnim, theme);
    }

    private void drawKey(GuiGraphics graphics, String label, float x, float y, float size, Animation anim, ThemeManager theme) {
        float press = anim.getValue();
        int bg = ColorUtils.lerp(ColorUtils.withAlpha(theme.getCard(), 200), ColorUtils.withAlpha(theme.getPrimary(), 230), press);
        RenderUtils.roundedRect(graphics, x, y, size, size, 4.0f, bg);
        int textColor = ColorUtils.lerp(theme.getText(), 0xFFFFFFFF, press);
        float textW = TextRenderer.width(label);
        TextRenderer.draw(graphics, label, x + (size - textW) / 2.0f, y + (size - TextRenderer.height()) / 2.0f, textColor, true);
    }

    private void drawMouseButton(GuiGraphics graphics, String label, float x, float y, float w, Animation anim, ThemeManager theme) {
        float press = anim.getValue();
        int bg = ColorUtils.lerp(ColorUtils.withAlpha(theme.getCard(), 200), ColorUtils.withAlpha(theme.getPrimary(), 230), press);
        RenderUtils.roundedRect(graphics, x, y, w, 24.0f, 4.0f, bg);
        int textColor = ColorUtils.lerp(theme.getText(), 0xFFFFFFFF, press);
        TextRenderer.draw(graphics, label, x + (w - TextRenderer.width(label)) / 2.0f, y + (24.0f - TextRenderer.height()) / 2.0f, textColor, true);
    }
}
