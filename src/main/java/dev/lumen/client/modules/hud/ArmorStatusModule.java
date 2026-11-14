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
    private final Animation[] durabilityAnims = new Animation[5];

    public ArmorStatusModule() {
        super("Armor Status", "Armor and held item durability");
        for (int i = 0; i < durabilityAnims.length; i++) {
            durabilityAnims[i] = new Animation(0.25f, Easing::easeOutCubic);
        }
    }

    @Override
    public float getHudWidth() {
        return 118.0f;
    }

    @Override
    public float getHudHeight() {
        return 5 * 24.0f;
    }

    @Override
    public void renderHud(GuiGraphics graphics, float delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ThemeManager theme = ThemeManager.get();

        ItemStack[] items = {
                mc.player.getItemBySlot(EquipmentSlot.HEAD),
                mc.player.getItemBySlot(EquipmentSlot.CHEST),
                mc.player.getItemBySlot(EquipmentSlot.LEGS),
                mc.player.getItemBySlot(EquipmentSlot.FEET),
                mc.player.getMainHandItem()
        };

        for (int i = 0; i < items.length; i++) {
            ItemStack stack = items[i];
            float y = i * 24.0f;
            if (stack == null || stack.isEmpty()) {
                durabilityAnims[i].setTarget(false);
                durabilityAnims[i].update();
                continue;
            }
            durabilityAnims[i].setTarget(true);
            durabilityAnims[i].update();
            float alpha = durabilityAnims[i].getValue();

            int bg = ColorUtils.withAlpha(theme.getCard(), (int) (210 * alpha));
            RenderUtils.roundedRect(graphics, 0.0f, y, 118.0f, 22.0f, 4.0f, bg);

            String name = stack.getHoverName().getString();
            if (name.length() > 12) name = name.substring(0, 12);
            TextRenderer.draw(graphics, name, 6.0f, y + 3.0f, ColorUtils.withAlpha(theme.getText(), (int) (255 * alpha)), true);

            int maxDamage = stack.getMaxDamage();
            if (maxDamage > 0) {
                float percent = 1.0f - (float) stack.getDamageValue() / maxDamage;
                int barColor = percent > 0.5f ? 0xFF5EFF8A : percent > 0.2f ? 0xFFFFD65E : 0xFFFF5E5E;
                RenderUtils.fill(graphics, 6.0f, y + 16.0f, 106.0f, 3.0f, ColorUtils.withAlpha(0xFF000000, (int) (160 * alpha)));
                RenderUtils.fill(graphics, 6.0f, y + 16.0f, 106.0f * percent, 3.0f, ColorUtils.withAlpha(barColor, (int) (255 * alpha)));
            }
        }
    }
}
