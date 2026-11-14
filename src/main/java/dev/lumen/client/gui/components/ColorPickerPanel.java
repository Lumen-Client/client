package dev.lumen.client.gui.components;

import dev.lumen.client.render.RenderUtils;
import dev.lumen.client.theme.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * HSB color picker: interactive color wheel, brightness bar, live preview
 * and hex input. State is cached - no per-frame allocation.
 */
public class ColorPickerPanel {
    public static final float WIDTH = 156.0f;
    public static final float HEIGHT = 178.0f;

    private boolean open;
    private float x;
    private float y;

    private IntSupplier getter;
    private IntConsumer setter;

    private float hue;
    private float sat = 1.0f;
    private float bri = 1.0f;

    private EditBox hexBox;
    private boolean draggingWheel;
    private boolean draggingBrightness;
    private boolean updatingHex;

    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
        draggingWheel = false;
        draggingBrightness = false;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public void open(IntSupplier getter, IntConsumer setter, float x, float y) {
        this.getter = getter;
        this.setter = setter;
        this.x = Math.min(x, Minecraft.getInstance().getWindow().getGuiScaledWidth() - WIDTH - 8.0f);
        this.y = Math.min(y, Minecraft.getInstance().getWindow().getGuiScaledHeight() - HEIGHT - 8.0f);
        int argb = getter.getAsInt();
        float[] hsb = ColorUtils.rgbToHsb(argb);
        hue = hsb[0];
        sat = hsb[1];
        bri = hsb[2];
        ensureHexBox();
        updatingHex = true;
        hexBox.setValue(ColorUtils.toHex(argb));
        updatingHex = false;
        open = true;
    }

    private void ensureHexBox() {
        if (hexBox == null) {
            hexBox = new EditBox(Minecraft.getInstance().font, 0, 0, 64, 14, Component.literal("#RRGGBB"));
            hexBox.setMaxLength(7);
            hexBox.setFilter(s -> s.isEmpty() || s.matches("^#?[0-9a-fA-F]{1,6}"));
            hexBox.setResponder(s -> {
                if (!updatingHex) {
                    int parsed = ColorUtils.parseHex(s);
                    if (parsed != -1) {
                        applyColor(parsed);
                    }
                }
            });
        }
    }

    private void applyColor(int argb) {
        if (setter != null) {
            setter.accept(argb | 0xFF000000);
        }
    }

    private int currentColor() {
        return ColorUtils.hsbToRgb(hue, sat, bri, 255);
    }

    private void commitCurrent() {
        applyColor(currentColor());
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta, float alpha) {
        if (!open) return;
        RenderUtils.dropShadow(graphics, x, y, WIDTH, HEIGHT);
        RenderUtils.roundedRect(graphics, x, y, WIDTH, HEIGHT, 8.0f, ColorUtils.withAlpha(0xFF14141A, (int) (255 * alpha)));

        float wheelSize = 96.0f;
        float wheelX = x + 8.0f;
        float wheelY = y + 20.0f;
        RenderUtils.colorWheel(graphics, wheelX, wheelY, wheelSize);

        float cx = wheelX + wheelSize / 2.0f;
        float cy = wheelY + wheelSize / 2.0f;
        float markerDist = sat * (wheelSize / 2.0f - 2.0f);
        double angle = hue * Math.PI * 2.0;
        float markerX = cx + (float) Math.cos(angle) * markerDist;
        float markerY = cy + (float) Math.sin(angle) * markerDist;
        RenderUtils.roundedRect(graphics, markerX - 2.5f, markerY - 2.5f, 5.0f, 5.0f, 2.5f, 0xFFFFFFFF);

        float barY = wheelY + wheelSize + 6.0f;
        RenderUtils.brightnessBar(graphics, wheelX, barY, wheelSize, 10.0f);
        float briX = wheelX + wheelSize * bri;
        RenderUtils.roundedRect(graphics, briX - 2.0f, barY - 2.0f, 4.0f, 14.0f, 2.0f, 0xFFFFFFFF);

        RenderUtils.roundedRect(graphics, x + 8.0f, y + HEIGHT - 26.0f, 16.0f, 16.0f, 4.0f, currentColor() | 0xFF000000);

        hexBox.setX((int) (x + 30.0f));
        hexBox.setY((int) (y + HEIGHT - 24.0f));
        hexBox.setWidth(64);
        hexBox.render(graphics, mouseX, mouseY, delta);
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (!open) return false;
        double mx = event.x();
        double my = event.y();

        float wheelSize = 96.0f;
        float wheelX = x + 8.0f;
        float wheelY = y + 20.0f;
        float cx = wheelX + wheelSize / 2.0f;
        float cy = wheelY + wheelSize / 2.0f;
        float dx = (float) (mx - cx);
        float dy = (float) (my - cy);
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist <= wheelSize / 2.0f + 2.0f) {
            draggingWheel = true;
            updateWheel(dx, dy, dist, wheelSize);
            return true;
        }

        float barY = wheelY + wheelSize + 6.0f;
        if (mx >= wheelX && mx <= wheelX + wheelSize && my >= barY - 3 && my <= barY + 13) {
            draggingBrightness = true;
            bri = Math.max(0.0f, Math.min(1.0f, (float) ((mx - wheelX) / wheelSize)));
            commitCurrent();
            return true;
        }

        if (hexBox.isMouseOver(mx, my)) {
            hexBox.setFocused(true);
            hexBox.mouseClicked(event, doubled);
            return true;
        }
        hexBox.setFocused(false);

        if (mx >= x && mx <= x + WIDTH && my >= y && my <= y + HEIGHT) {
            return true;
        }
        close();
        return false;
    }

    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!open) return false;
        float wheelSize = 96.0f;
        float wheelX = x + 8.0f;
        float wheelY = y + 20.0f;
        float cx = wheelX + wheelSize / 2.0f;
        float cy = wheelY + wheelSize / 2.0f;
        float dx = (float) (event.x() - cx);
        float dy = (float) (event.y() - cy);
        if (draggingWheel) {
            updateWheel(dx, dy, (float) Math.sqrt(dx * dx + dy * dy), wheelSize);
            return true;
        }
        if (draggingBrightness) {
            bri = Math.max(0.0f, Math.min(1.0f, (float) ((event.x() - wheelX) / wheelSize)));
            commitCurrent();
            return true;
        }
        return false;
    }

    public boolean mouseReleased() {
        boolean was = draggingWheel || draggingBrightness;
        draggingWheel = false;
        draggingBrightness = false;
        return was;
    }

    private void updateWheel(float dx, float dy, float dist, float wheelSize) {
        double angle = Math.atan2(dy, dx);
        if (angle < 0) angle += Math.PI * 2.0;
        hue = (float) (angle / (Math.PI * 2.0));
        sat = Math.min(1.0f, dist / (wheelSize / 2.0f - 2.0f));
        commitCurrent();
    }

    public boolean keyPressed(KeyEvent event) {
        if (!open) return false;
        if (event.key() == 256) {
            close();
            return true;
        }
        return hexBox.isFocused() && hexBox.keyPressed(event);
    }

    public boolean charTyped(CharacterEvent event) {
        if (!open) return false;
        return hexBox.charTyped(event);
    }
}
