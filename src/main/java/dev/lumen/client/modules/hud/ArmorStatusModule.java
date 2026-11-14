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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class ArmorStatusModule extends HudModule {
    private static final int SLOT_COUNT = 5;
    private static final float ROW_H = 24.0f;

    private final Animation[] durabilityAnims = new Animation[SLOT_COUNT];
    private boolean anyVisible;

    public ArmorStatusModule() {
        super("Armor Status", "Armor and held item durability with numeric values", 300.0f, 6.0f);
        for (int i = 0; i < durabilityAnims.length; i++) {
            durabilityAnims[i] = new Animation(0.25f, Easing::easeOutCubic);
        }
    }

    private ItemStack[] collectItems() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        return new ItemStack[]{
                mc.player.getItemBySlot(EquipmentSlot.HEAD),
                mc.player.getItemBySlot(EquipmentSlot.CHEST),
                mc.player.getItemBySlot(EquipmentSlot.LEGS),
                mc.player.getItemBySlot(EquipmentSlot.FEET),
                mc.player.getMainHandItem()
        };
    }

    @Override
    public float getHudWidth() {
        return 118.0f;
    }

    @Override
    public float getHudHeight() {
        return SLOT_COUNT * ROW_H;
    }

    @Override
    public boolean isContentEmpty() {
        return !anyVisible;
    }

    @Override
    public void renderHud(GuiGraphics graphics, float delta) {
        ItemStack[] items = collectItems();
        if (items == null) {
            anyVisible = false;
            return;
        }
        ThemeManager theme = ThemeManager.get();

        anyVisible = false;
        float rowY = 0.0f;
        for (int i = 0; i < items.length; i++) {
            ItemStack stack = items[i];
            if (stack == null || stack.isEmpty()) {
                durabilityAnims[i].setTarget(false);
                durabilityAnims[i].update();
                continue;
            }
            anyVisible = true;
            durabilityAnims[i].setTarget(true);
            durabilityAnims[i].update();
            float alpha = durabilityAnims[i].getValue();
            float y = rowY;
            rowY += ROW_H;

            int bg = ColorUtils.withAlpha(theme.getCard(), (int) (210 * alpha));
            RenderUtils.roundedRect(graphics, 0.0f, y, 118.0f, 22.0f, 4.0f, bg);

            String name = stack.getHoverName().getString();
            if (name.length() > 10) name = name.substring(0, 10);
            TextRenderer.draw(graphics, name, 6.0f, y + 3.0f, ColorUtils.withAlpha(theme.getText(), (int) (255 * alpha)), true);

            int maxDamage = stack.getMaxDamage();
            if (maxDamage > 0) {
                int remaining = maxDamage - stack.getDamageValue();
                float percent = (float) remaining / maxDamage;
                int barColor = percent > 0.5f ? 0xFF5EFF8A : percent > 0.2f ? 0xFFFFD65E : 0xFFFF5E5E;
                RenderUtils.fill(graphics, 6.0f, y + 16.0f, 106.0f, 3.0f, ColorUtils.withAlpha(0xFF000000, (int) (160 * alpha)));
                RenderUtils.fill(graphics, 6.0f, y + 16.0f, 106.0f * percent, 3.0f, ColorUtils.withAlpha(barColor, (int) (255 * alpha)));

                String durability = remaining + "/" + maxDamage;
                float durW = TextRenderer.width(durability);
                TextRenderer.draw(graphics, durability, 112.0f - durW, y + 3.0f,
                        ColorUtils.withAlpha(barColor, (int) (255 * alpha)), true);
            }
        }
    }
}
