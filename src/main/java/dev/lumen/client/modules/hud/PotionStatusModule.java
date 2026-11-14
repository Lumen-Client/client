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
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PotionStatusModule extends HudModule {
    private static final class Row {
        final Animation anim = new Animation(0.2f, Easing::easeOutCubic);
        String label = "";
        int color;
        boolean active;
    }

    private final List<Row> rows = new ArrayList<>();
    private int lastRowCount;

    public PotionStatusModule() {
        super("Potion Status", "Active effect timers");
    }

    @Override
    public float getHudWidth() {
        return 130.0f;
    }

    @Override
    public float getHudHeight() {
        int count = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            count = Math.min(mc.player.getActiveEffects().size(), 8);
        }
        return Math.max(count, 1) * 22.0f;
    }

    @Override
    public void renderHud(GuiGraphics graphics, float delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ThemeManager theme = ThemeManager.get();

        Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
        int index = 0;
        for (MobEffectInstance instance : effects) {
            if (index >= rows.size()) rows.add(new Row());
            Row row = rows.get(index);
            Holder<MobEffect> effect = instance.getEffect();
            String name = effect.value().getDisplayName().getString();
            int amp = instance.getAmplifier();
            String label = name + (amp > 0 ? " " + (amp + 1) : "") + " " + (instance.getDuration() / 20) + "s";
            row.label = label;
            row.color = effect.value().getColor();
            row.anim.setTarget(true);
            row.anim.update();
            renderRow(graphics, row, index * 22.0f, theme);
            index++;
            if (index >= 8) break;
        }
        for (int i = index; i < rows.size() && i < lastRowCount; i++) {
            Row row = rows.get(i);
            row.anim.setTarget(false);
            row.anim.update();
            if (row.anim.getValue() > 0.02f) {
                renderRow(graphics, row, i * 22.0f, theme);
            }
        }
        lastRowCount = index;
    }

    private void renderRow(GuiGraphics graphics, Row row, float y, ThemeManager theme) {
        float alpha = row.anim.getValue();
        int bg = ColorUtils.withAlpha(theme.getCard(), (int) (210 * alpha));
        RenderUtils.roundedRect(graphics, 0.0f, y, 130.0f, 20.0f, 4.0f, bg);
        RenderUtils.fill(graphics, 0.0f, y, 2.5f, 20.0f, ColorUtils.withAlpha(row.color, (int) (255 * alpha)));
        TextRenderer.draw(graphics, row.label, 7.0f, y + 5.0f, ColorUtils.withAlpha(theme.getText(), (int) (255 * alpha)), true);
    }
}
