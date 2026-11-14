package dev.lumen.client.modules.hud;

import dev.lumen.client.core.HudModule;
import dev.lumen.client.render.TextRenderer;
import dev.lumen.client.theme.ColorUtils;
import dev.lumen.client.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public class CoordinatesModule extends HudModule {
    private final dev.lumen.client.core.setting.BooleanSetting showBiome =
            new dev.lumen.client.core.setting.BooleanSetting("Show Biome", true);
    private final dev.lumen.client.core.setting.BooleanSetting netherConverter =
            new dev.lumen.client.core.setting.BooleanSetting("Nether Converter", true);

    public CoordinatesModule() {
        super("Coordinates", "XYZ, dimension converter and biome", 6.0f, 84.0f);
        addSettings(showBiome, netherConverter);
    }

    @Override
    public float getHudWidth() {
        return 150.0f;
    }

    @Override
    public float getHudHeight() {
        Minecraft mc = Minecraft.getInstance();
        int lines = 1;
        if (netherConverter.get()) lines++;
        if (showBiome.get() && mc.level != null) lines++;
        return lines * (TextRenderer.height() + 3.0f) + 6.0f;
    }

    @Override
    public void renderHud(GuiGraphics graphics, float delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ThemeManager theme = ThemeManager.get();
        int text = theme.getText();
        int accent = theme.getPrimary();

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float lineH = TextRenderer.height() + 3.0f;
        float cy = 3.0f;

        TextRenderer.draw(graphics, String.format("XYZ %.0f %.0f %.0f", x, y, z), 5.0f, cy, ColorUtils.withAlpha(text, 255), true);
        cy += lineH;

        if (netherConverter.get()) {
            ResourceKey<Level> dimension = mc.level.dimension();
            if (dimension == Level.NETHER) {
                TextRenderer.draw(graphics, String.format("Overworld %.0f %.0f", x * 8.0, z * 8.0), 5.0f, cy, ColorUtils.withAlpha(accent, 230), true);
            } else if (dimension == Level.OVERWORLD) {
                TextRenderer.draw(graphics, String.format("Nether %.0f %.0f", x / 8.0, z / 8.0), 5.0f, cy, ColorUtils.withAlpha(accent, 230), true);
            } else {
                TextRenderer.draw(graphics, "No conversion", 5.0f, cy, ColorUtils.withAlpha(text, 140), true);
            }
            cy += lineH;
        }

        if (showBiome.get() && mc.level != null) {
            BlockPos pos = mc.player.blockPosition();
            Holder<Biome> biome = mc.level.getBiome(pos);
            String biomeName = biome.unwrapKey().map(k -> k.identifier().getPath()).orElse("unknown");
            TextRenderer.draw(graphics, "Biome " + biomeName, 5.0f, cy, ColorUtils.withAlpha(text, 210), true);
        }
    }
}
