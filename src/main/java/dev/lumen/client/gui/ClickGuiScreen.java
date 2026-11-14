package dev.lumen.client.gui;

import dev.lumen.client.Lumen;
import dev.lumen.client.LumenClient;
import dev.lumen.client.anim.Animation;
import dev.lumen.client.anim.AnimationUtils;
import dev.lumen.client.anim.Easing;
import dev.lumen.client.core.Category;
import dev.lumen.client.core.Module;
import dev.lumen.client.core.config.ConfigManager;
import dev.lumen.client.core.setting.BooleanSetting;
import dev.lumen.client.core.setting.ColorSetting;
import dev.lumen.client.core.setting.EnumSetting;
import dev.lumen.client.core.setting.FontSetting;
import dev.lumen.client.core.setting.NumberSetting;
import dev.lumen.client.core.setting.Setting;
import dev.lumen.client.font.FontManager;
import dev.lumen.client.gui.components.ColorPickerPanel;
import dev.lumen.client.gui.components.Slider;
import dev.lumen.client.gui.components.ToggleSwitch;
import dev.lumen.client.modules.perf.SystemOptimizerModule;
import dev.lumen.client.modules.visual.CrosshairModule;
import dev.lumen.client.render.RenderUtils;
import dev.lumen.client.render.TextRenderer;
import dev.lumen.client.theme.ColorUtils;
import dev.lumen.client.theme.Theme;
import dev.lumen.client.theme.ThemeManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lumen ClickGUI: glassmorphic centered modal with gliding sidebar tabs,
 * search, accordion settings, animated controls, Appearance/Profiles panels
 * and a Performance dashboard with RAM monitor.
 */
public class ClickGuiScreen extends Screen {
    private static final float WIN_W = 470.0f;
    private static final float WIN_H = 282.0f;
    private static final float SIDEBAR_W = 118.0f;
    private static final float HEADER_H = 24.0f;
    private static final float TAB_H = 30.0f;
    private static final float ROW_H = 26.0f;
    private static final float SETTING_H = 28.0f;

    private static final String[] TABS = {"HUD", "Visuals", "QoL", "Performance", "Appearance", "Profiles"};
    private static final Category[] TAB_CATEGORIES = {Category.HUD, Category.VISUALS, Category.QOL, Category.PERFORMANCE};

    private final Screen parent;
    private final Animation scaleAnim = new Animation(0.24f, Easing::easeOutBack);
    private final Animation fadeAnim = new Animation(0.18f, Easing::easeOutCubic);
    private final Animation expandAnim = new Animation(0.2f, Easing::easeOutCubic);
    private boolean closing;

    private int activeTab;
    private float indicatorY;
    private long lastNanos = System.nanoTime();

    private String search = "";
    private boolean searchFocused;

    private Module expanded;
    private Module waitingKeybind;

    private float scroll;
    private float scrollTarget;
    private float maxScroll;

    private final Map<Object, ToggleSwitch> toggleCache = new HashMap<>();
    private final Map<Object, Slider> sliderCache = new HashMap<>();
    private final ColorPickerPanel colorPicker = new ColorPickerPanel();

    private final NumberSetting gradientSpeedSetting = new NumberSetting("Gradient Speed", 1.0, 0.1, 3.0, 0.05, 2);
    private final NumberSetting fontScaleSetting = new NumberSetting("Font Scale", 1.0, 0.5, 2.0, 0.05, 2);
    private final NumberSetting guiOpacitySetting = new NumberSetting("Background Opacity", 1.0, 0.3, 1.0, 0.05, 2);
    private final NumberSetting guiScaleSetting = new NumberSetting("GUI Scale", 1.0, 0.7, 1.6, 0.05, 2);
    private final Map<NumberSetting, float[]> sliderGeom = new HashMap<>();
    private final Map<NumberSetting, float[]> appearanceSliderGeom = new HashMap<>();
    private int fontIndex;

    private EditBox profileBox;
    private List<String> cachedProfiles = new ArrayList<>();

    private float contentX;
    private float contentY;
    private float contentW;
    private float contentH;
    private float gcButtonY = -1.0f;
    private float closeX;
    private float closeY;

    private final float[] presetHitboxes = new float[12];
    private final float[] themeSwatchY = new float[4];
    private float gradientModeY = -1.0f;
    private float fontSelectorY = -1.0f;
    private float crosshairPreviewY = -1.0f;
    private float saveButtonY = -1.0f;
    private final List<Float> profileRowYs = new ArrayList<>();
    private float guiK = 1.0f;
    private float hudEditorSidebarY = -1.0f;
    private String tooltipText;

    public ClickGuiScreen(Screen parent) {
        super(Component.literal("Lumen Client"));
        this.parent = parent;
        dev.lumen.client.sfx.ClientSounds.guiOpen();
    }

    @Override
    protected void init() {
        scaleAnim.snap(false);
        scaleAnim.setTarget(true);
        fadeAnim.snap(false);
        fadeAnim.setTarget(true);
        if (profileBox == null) {
            profileBox = new EditBox(minecraft.font, 0, 0, 150, 15, Component.literal("Profile name"));
            profileBox.setMaxLength(24);
        }
        guiOpacitySetting.set(ThemeManager.get().getGuiOpacity());
        guiScaleSetting.set(ThemeManager.get().getGuiScale());
        FontManager fonts = FontManager.get();
        fontScaleSetting.set(fonts.getFontScale());
        List<String> ids = fontIds();
        fontIndex = Math.max(0, ids.indexOf(fonts.isVanillaFont() ? FontManager.VANILLA_ID : fonts.getSelectedFontId()));
    }

    private List<String> fontIds() {
        List<String> ids = new ArrayList<>();
        ids.add(FontManager.VANILLA_ID);
        ids.addAll(FontManager.get().getFontIds());
        return ids;
    }

    private void startClosing() {
        if (!closing) {
            dev.lumen.client.sfx.ClientSounds.guiClose();
        }
        closing = true;
        scaleAnim.setTarget(false);
        fadeAnim.setTarget(false);
        colorPicker.close();
    }

    private void updateAnimations() {
        float delta = AnimationUtils.deltaSeconds(lastNanos);
        lastNanos = System.nanoTime();
        scaleAnim.update();
        fadeAnim.update();
        expandAnim.update();
        float target = activeTab * TAB_H;
        indicatorY = AnimationUtils.approach(indicatorY, target, 22.0f, delta);
        scroll = AnimationUtils.approach(scroll, scrollTarget, 20.0f, delta);
        guiScaleSetting.set(ThemeManager.get().getGuiScale());
        guiOpacitySetting.set(ThemeManager.get().getGuiOpacity());
    }

    /**
     * The GUI renders at a constant real-world size regardless of Minecraft's
     * GUI Scale option; the Appearance tab slider scales it further.
     */
    private float computeGuiK() {
        float windowScale = Math.max(1.0f, minecraft.getWindow().getGuiScale());
        return (windowScale / 2.0f) * guiScaleSetting.getFloat();
    }

    private double toLocalX(double x) {
        return width / 2.0f + (x - width / 2.0f) / guiK;
    }

    private double toLocalY(double y) {
        return height / 2.0f + (y - height / 2.0f) / guiK;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        updateAnimations();
        float alpha = fadeAnim.getValue();
        if (closing && alpha <= 0.01f) {
            minecraft.setScreen(parent);
            return;
        }

        RenderUtils.fill(graphics, 0, 0, width, height, ColorUtils.withAlpha(0x66000000, (int) (255 * alpha)));

        guiK = computeGuiK();
        float winX = (width - WIN_W) / 2.0f;
        float winY = (height - WIN_H) / 2.0f;
        float scale = (0.92f + 0.08f * scaleAnim.getValue()) * guiK;
        float cx = width / 2.0f;
        float cy = height / 2.0f;

        float localMouseX = (float) toLocalX(mouseX);
        float localMouseY = (float) toLocalY(mouseY);

        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-cx, -cy);

        ThemeManager theme = ThemeManager.get();
        tooltipText = null;
        RenderUtils.dropShadow(graphics, winX, winY, WIN_W, WIN_H);
        RenderUtils.roundedRect(graphics, winX - 1.0f, winY - 1.0f, WIN_W + 2.0f, WIN_H + 2.0f, 11.0f,
                ColorUtils.withAlpha(theme.getPrimary(), (int) (90 * alpha)));
        RenderUtils.roundedRect(graphics, winX, winY, WIN_W, WIN_H, 10.0f,
                ColorUtils.withAlpha(theme.getBackground(), (int) (242 * alpha * theme.getGuiOpacity())));
        RenderUtils.roundedGradient(graphics, winX, winY, WIN_W, HEADER_H, 10.0f,
                theme.gradientColor(0.0f, alpha * 0.35f), theme.gradientColor(1.0f, alpha * 0.35f), true);

        RenderUtils.drawTexture(graphics, RenderUtils.LOGO_TEXTURE, winX + 9.0f, winY + 5.0f, 14.0f, 14.0f,
                ColorUtils.withAlpha(0xFFFFFF, (int) (255 * alpha)));
        TextRenderer.drawScaled(graphics, "Lumen Client", winX + 27.0f, winY + 8.0f,
                theme.gradientColor(0.0f, alpha), 1.1f, true);
        TextRenderer.drawScaled(graphics, "v" + Lumen.VERSION, winX + 112.0f, winY + 9.0f,
                ColorUtils.withAlpha(theme.getText(), (int) (140 * alpha)), 0.8f, true);

        closeX = winX + WIN_W - 18.0f;
        closeY = winY;
        boolean closeHover = mouseX >= closeX - 6 && mouseX <= closeX + 12 && mouseY >= closeY && mouseY <= closeY + HEADER_H;
        TextRenderer.draw(graphics, "\u2715", closeX, winY + 7.0f,
                ColorUtils.withAlpha(closeHover ? 0xFFFF4646 : theme.getText(), (int) (220 * alpha)), true);

        renderSidebar(graphics, (int) localMouseX, (int) localMouseY, winX, winY, alpha);
        renderContent(graphics, (int) localMouseX, (int) localMouseY, winX, winY, alpha, delta);

        colorPicker.render(graphics, (int) localMouseX, (int) localMouseY, delta, alpha);

        if (tooltipText != null && !tooltipText.isEmpty()) {
            float tw = TextRenderer.width(tooltipText);
            float tx = Math.min(localMouseX + 12.0f, width - tw - 18.0f);
            float ty = Math.min(localMouseY + 16.0f, height - 24.0f);
            RenderUtils.roundedRect(graphics, tx - 7.0f, ty - 5.0f, tw + 14.0f, TextRenderer.height() + 10.0f, 6.0f,
                    ColorUtils.withAlpha(theme.getPrimary(), (int) (120 * alpha)));
            RenderUtils.roundedRect(graphics, tx - 6.0f, ty - 4.0f, tw + 12.0f, TextRenderer.height() + 8.0f, 5.0f,
                    ColorUtils.withAlpha(0xFF0B0B0E, (int) (242 * alpha)));
            TextRenderer.draw(graphics, tooltipText, tx, ty,
                    ColorUtils.withAlpha(theme.getText(), (int) (235 * alpha)), true);
        }

        graphics.pose().popMatrix();
    }

    private void renderSidebar(GuiGraphics graphics, int mouseX, int mouseY, float winX, float winY, float alpha) {
        ThemeManager theme = ThemeManager.get();
        float sidebarY = winY + HEADER_H;

        RenderUtils.roundedRect(graphics, winX + 4.0f, sidebarY + 4.0f, SIDEBAR_W - 8.0f, WIN_H - HEADER_H - 8.0f, 8.0f,
                ColorUtils.withAlpha(theme.getCard(), (int) (200 * alpha)));

        for (int i = 0; i < TABS.length; i++) {
            float tabY = sidebarY + 6.0f + i * TAB_H;
            boolean active = i == activeTab;
            boolean hover = mouseX >= winX + 4 && mouseX <= winX + SIDEBAR_W - 4
                    && mouseY >= tabY && mouseY <= tabY + TAB_H;

            if (active) {
                RenderUtils.roundedGradient(graphics, winX + 8.0f, indicatorY + sidebarY + 9.0f, 3.0f, TAB_H - 6.0f, 1.5f,
                        theme.gradientColor(0.0f, alpha), theme.gradientColor(1.0f, alpha), true);
            }
            int color = active ? theme.gradientColor(0.5f, alpha)
                    : ColorUtils.withAlpha(theme.getText(), (int) ((hover ? 200 : 130) * alpha));
            TextRenderer.draw(graphics, TABS[i], winX + 20.0f, tabY + TAB_H / 2.0f - TextRenderer.height() / 2.0f, color, true);
        }

        hudEditorSidebarY = sidebarY + 6.0f + TABS.length * TAB_H + 8.0f;
        boolean editorHover = mouseX >= winX + 8 && mouseX <= winX + SIDEBAR_W - 8
                && mouseY >= hudEditorSidebarY && mouseY <= hudEditorSidebarY + 22.0f;
        RenderUtils.roundedGradient(graphics, winX + 8.0f, hudEditorSidebarY, SIDEBAR_W - 16.0f, 22.0f, 6.0f,
                theme.gradientColor(0.0f, alpha * (editorHover ? 1.0f : 0.7f)),
                theme.gradientColor(1.0f, alpha * (editorHover ? 1.0f : 0.7f)), false);
        String editorLabel = "HUD Editor";
        TextRenderer.draw(graphics, editorLabel,
                winX + 8.0f + (SIDEBAR_W - 16.0f - TextRenderer.width(editorLabel)) / 2.0f,
                hudEditorSidebarY + 11.0f - TextRenderer.height() / 2.0f,
                ColorUtils.withAlpha(0xFFFFFFFF, (int) (255 * alpha)), true);
        if (editorHover) {
            tooltipText = "Rearrange HUD elements: drag to move, scroll to scale";
        }
    }

    private void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float winX, float winY, float alpha, float delta) {
        ThemeManager theme = ThemeManager.get();
        contentX = winX + SIDEBAR_W + 6.0f;
        contentY = winY + HEADER_H + 6.0f;
        contentW = WIN_W - SIDEBAR_W - 16.0f;
        contentH = WIN_H - HEADER_H - 12.0f;

        RenderUtils.roundedRect(graphics, contentX, contentY, contentW, contentH, 8.0f,
                ColorUtils.withAlpha(theme.getCard(), (int) (170 * alpha)));

        graphics.enableScissor((int) contentX, (int) contentY, (int) (contentX + contentW), (int) (contentY + contentH));

        maxScroll = 0.0f;
        if (activeTab <= 3) {
            renderModuleTab(graphics, mouseX, mouseY, alpha);
        } else if (activeTab == 4) {
            renderAppearanceTab(graphics, mouseX, mouseY, alpha, delta);
        } else {
            renderProfilesTab(graphics, mouseX, mouseY, alpha, delta);
        }

        graphics.disableScissor();
    }

    private List<Module> filteredModules() {
        List<Module> modules = LumenClient.get().getModuleManager().getByCategory(TAB_CATEGORIES[activeTab]);
        if (search.isEmpty()) return modules;
        List<Module> result = new ArrayList<>();
        String query = search.toLowerCase(Locale.ROOT);
        for (Module module : modules) {
            if (module.getName().toLowerCase(Locale.ROOT).contains(query)) {
                result.add(module);
            }
        }
        return result;
    }

    private void renderModuleTab(GuiGraphics graphics, int mouseX, int mouseY, float alpha) {
        ThemeManager theme = ThemeManager.get();

        renderSearchBar(graphics, mouseX, mouseY, alpha);
        float y = contentY + 26.0f - scroll;

        List<Module> modules = filteredModules();
        float listBottom = contentY + 26.0f;
        for (Module module : modules) {
            float rowHeight = ROW_H;
            if (module == expanded) {
                rowHeight += expandAnim.getValue() * settingsHeight(module);
            }
            listBottom += rowHeight;
            if (y + rowHeight < contentY || y > contentY + contentH) {
                y += rowHeight;
                continue;
            }
            renderModuleRow(graphics, mouseX, mouseY, module, y, alpha);
            y += rowHeight;
        }

        if (activeTab == 3) {
            listBottom = renderPerformanceExtras(graphics, mouseX, mouseY, y + 6.0f, alpha, listBottom);
        }

        maxScroll = Math.max(0.0f, listBottom - contentY - contentH);
        scrollTarget = Math.max(0.0f, Math.min(maxScroll, scrollTarget));
    }

    private float renderPerformanceExtras(GuiGraphics graphics, int mouseX, int mouseY, float y, float alpha, float listBottom) {
        ThemeManager theme = ThemeManager.get();
        SystemOptimizerModule system = SystemOptimizerModule.get();
        if (system == null) return listBottom;

        float used = system.getUsedHeapFraction();
        float allocated = system.getAllocatedFraction();

        float barX = contentX + 8.0f;
        float barW = contentW - 16.0f;
        RenderUtils.roundedRect(graphics, barX, y, barW, 10.0f, 5.0f, ColorUtils.withAlpha(0xFF2A2A33, (int) (255 * alpha)));
        int usedColor = used > 0.9f ? 0xFFFF4646 : theme.getPrimary();
        RenderUtils.roundedGradient(graphics, barX, y, Math.max(10.0f, barW * used), 10.0f, 5.0f,
                ColorUtils.scaleAlpha(usedColor, alpha), theme.gradientColor(1.0f, alpha), false);
        RenderUtils.border(graphics, barX, y, barW * allocated, 10.0f, 1.0f, ColorUtils.withAlpha(theme.getText(), (int) (70 * alpha)));
        TextRenderer.draw(graphics, "RAM " + Math.round(used * 100.0f) + "% used / "
                        + Math.round(allocated * 100.0f) + "% allocated",
                barX, y + 14.0f, ColorUtils.withAlpha(theme.getText(), (int) (200 * alpha)), true);

        float buttonY = y + 30.0f;
        boolean hover = mouseX >= barX && mouseX <= barX + barW && mouseY >= buttonY && mouseY <= buttonY + 20.0f;
        RenderUtils.roundedRect(graphics, barX, buttonY, barW, 20.0f, 6.0f,
                ColorUtils.withAlpha(hover ? theme.getPrimary() : theme.getBackground(), (int) (220 * alpha)));
        String label = "Free Memory (GC)";
        TextRenderer.draw(graphics, label, barX + (barW - TextRenderer.width(label)) / 2.0f,
                buttonY + 10.0f - TextRenderer.height() / 2.0f, ColorUtils.withAlpha(theme.getText(), (int) (240 * alpha)), true);
        gcButtonY = buttonY;

        return listBottom + 58.0f;
    }

    private void renderSearchBar(GuiGraphics graphics, int mouseX, int mouseY, float alpha) {
        ThemeManager theme = ThemeManager.get();
        float barX = contentX + 8.0f;
        float barY = contentY + 6.0f;
        float barW = contentW - 16.0f;

        RenderUtils.roundedRect(graphics, barX - 1.0f, barY - 1.0f, barW + 2.0f, 18.0f, 6.0f,
                ColorUtils.withAlpha(theme.getPrimary(), (int) ((searchFocused ? 160 : 50) * alpha)));
        RenderUtils.roundedRect(graphics, barX, barY, barW, 16.0f, 5.0f,
                ColorUtils.withAlpha(theme.getBackground(), (int) (255 * alpha)));

        String cursor = searchFocused && (System.nanoTime() / 500_000_000L) % 2 == 0 ? "_" : "";
        String display = search.isEmpty() && !searchFocused ? "Search modules..." : search + cursor;
        int textAlpha = (int) ((search.isEmpty() && !searchFocused ? 110 : 235) * alpha);
        TextRenderer.draw(graphics, display, barX + 6.0f, barY + 8.0f - TextRenderer.height() / 2.0f,
                ColorUtils.withAlpha(theme.getText(), textAlpha), true);
    }

    private void renderModuleRow(GuiGraphics graphics, int mouseX, int mouseY, Module module, float y, float alpha) {
        ThemeManager theme = ThemeManager.get();
        float rowX = contentX + 8.0f;
        float rowW = contentW - 16.0f;

        boolean hover = mouseX >= rowX && mouseX <= rowX + rowW && mouseY >= y && mouseY <= y + ROW_H;
        if (hover) {
            tooltipText = module.getDescription();
        }
        int cardColor = ColorUtils.withAlpha(hover ? ColorUtils.brighten(theme.getCard(), 0.06f) : theme.getCard(), (int) (235 * alpha));
        RenderUtils.roundedRect(graphics, rowX, y, rowW, ROW_H, 6.0f, cardColor);
        if (module.isEnabled()) {
            RenderUtils.roundedGradient(graphics, rowX, y, 3.0f, ROW_H, 1.5f,
                    theme.gradientColor(0.0f, alpha), theme.gradientColor(1.0f, alpha), true);
        }

        int nameColor = module.isEnabled() ? theme.getText() : ColorUtils.withAlpha(theme.getText(), (int) (170 * alpha));
        TextRenderer.draw(graphics, module.getName(), rowX + 10.0f, y + ROW_H / 2.0f - TextRenderer.height() / 2.0f, nameColor, true);

        if (module.isBound()) {
            String keyHint = keyName(module.getKeyCode());
            float hintW = TextRenderer.width(keyHint);
            TextRenderer.draw(graphics, keyHint, rowX + rowW - 56.0f - hintW, y + ROW_H / 2.0f - TextRenderer.height() / 2.0f,
                    ColorUtils.withAlpha(theme.getText(), (int) (110 * alpha)), true);
        }

        ToggleSwitch toggle = toggleCache.computeIfAbsent(module, k -> new ToggleSwitch());
        toggle.render(graphics, rowX + rowW - 42.0f, y + ROW_H / 2.0f - 8.0f, 34.0f, 16.0f, module.isEnabled(), alpha);

        if (module == expanded && expandAnim.getValue() > 0.01f) {
            float settingsH = expandAnim.getValue() * settingsHeight(module);
            RenderUtils.roundedRect(graphics, rowX, y + ROW_H, rowW, settingsH, 5.0f,
                    ColorUtils.withAlpha(ColorUtils.darken(theme.getCard(), 0.03f), (int) (235 * alpha)));
            float sy = y + ROW_H;
            float visible = settingsH;
            for (Setting setting : module.getSettings()) {
                if (!setting.isVisible()) continue;
                float h = Math.min(SETTING_H, visible);
                renderSettingRow(graphics, setting, rowX, sy, rowW, alpha, h, mouseX, mouseY);
                sy += h;
                visible -= h;
                if (visible <= 0.01f) break;
            }
            if (visible > 0.01f) {
                renderKeybindRow(graphics, module, rowX, sy, rowW, alpha, Math.min(SETTING_H, visible));
            }
        }
    }

    private float settingsHeight(Module module) {
        float height = SETTING_H;
        for (Setting setting : module.getSettings()) {
            if (setting.isVisible()) height += SETTING_H;
        }
        return height;
    }

    private void renderSettingRow(GuiGraphics graphics, Setting setting, float x, float y, float w, float alpha, float clipH,
                                  int mouseX, int mouseY) {
        ThemeManager theme = ThemeManager.get();
        int text = ColorUtils.withAlpha(theme.getText(), (int) (220 * alpha));
        float padding = 12.0f;

        if (mouseY >= y && mouseY <= y + clipH && mouseX >= x && mouseX <= x + w) {
            tooltipText = setting.getDescription();
        }

        if (setting instanceof BooleanSetting bool) {
            TextRenderer.draw(graphics, setting.getName(), x + padding, y + clipH / 2.0f - TextRenderer.height() / 2.0f, text, true);
            ToggleSwitch toggle = toggleCache.computeIfAbsent(setting, k -> new ToggleSwitch());
            toggle.render(graphics, x + w - 46.0f, y + clipH / 2.0f - 8.0f, 34.0f, 16.0f, bool.get(), alpha);
        } else if (setting instanceof NumberSetting number) {
            Slider slider = sliderCache.computeIfAbsent(setting, k -> new Slider());
            float sx = x + padding;
            float sy2 = y + clipH - 10.0f;
            float sw = w - padding * 2.0f;
            slider.render(graphics, sx, sy2, sw, 7.0f, number, alpha);
            sliderGeom.put(number, new float[]{sx, sy2, sw, 7.0f});
        } else if (setting instanceof EnumSetting<?> enumSetting) {
            TextRenderer.draw(graphics, setting.getName(), x + padding, y + clipH / 2.0f - TextRenderer.height() / 2.0f, text, true);
            drawValueChip(graphics, enumSetting.get().name(), x + w - 90.0f, y + clipH / 2.0f - 8.0f, 82.0f, 16.0f, alpha);
        } else if (setting instanceof ColorSetting color) {
            TextRenderer.draw(graphics, setting.getName(), x + padding, y + clipH / 2.0f - TextRenderer.height() / 2.0f, text, true);
            int swatch = color.isSyncWithTheme() ? theme.getPrimary() : color.getRaw();
            RenderUtils.roundedRect(graphics, x + w - 47.0f, y + clipH / 2.0f - 9.0f, 36.0f, 18.0f, 6.0f,
                    ColorUtils.withAlpha(0xFFFFFFFF, (int) (70 * alpha)));
            RenderUtils.roundedRect(graphics, x + w - 46.0f, y + clipH / 2.0f - 8.0f, 34.0f, 16.0f, 5.0f, swatch | 0xFF000000);
        } else if (setting instanceof FontSetting font) {
            TextRenderer.draw(graphics, setting.getName(), x + padding, y + clipH / 2.0f - TextRenderer.height() / 2.0f, text, true);
            drawValueChip(graphics, FontManager.get().getFontDisplayName(font.get()), x + w - 120.0f, y + clipH / 2.0f - 8.0f, 108.0f, 16.0f, alpha);
        } else {
            TextRenderer.draw(graphics, setting.getName(), x + padding, y + clipH / 2.0f - TextRenderer.height() / 2.0f, text, true);
        }
    }

    private void drawValueChip(GuiGraphics graphics, String value, float x, float y, float w, float h, float alpha) {
        ThemeManager theme = ThemeManager.get();
        RenderUtils.roundedRect(graphics, x, y, w, h, 5.0f, ColorUtils.withAlpha(theme.getBackground(), (int) (255 * alpha)));
        RenderUtils.border(graphics, x, y, w, h, 1.0f, ColorUtils.withAlpha(theme.getPrimary(), (int) (80 * alpha)));
        String clipped = value;
        while (clipped.length() > 1 && TextRenderer.width(clipped) > w - 10.0f) {
            clipped = clipped.substring(0, clipped.length() - 1);
        }
        TextRenderer.draw(graphics, clipped, x + 5.0f, y + h / 2.0f - TextRenderer.height() / 2.0f,
                ColorUtils.withAlpha(theme.getText(), (int) (230 * alpha)), true);
    }

    private void renderKeybindRow(GuiGraphics graphics, Module module, float x, float y, float w, float alpha, float clipH) {
        ThemeManager theme = ThemeManager.get();
        boolean waiting = waitingKeybind == module;
        String label = waiting ? "> press a key <"
                : "Keybind: " + (module.isBound() ? keyName(module.getKeyCode()) : "None");
        int color = waiting ? theme.gradientColor(0.5f, alpha)
                : ColorUtils.withAlpha(theme.getText(), (int) (190 * alpha));
        TextRenderer.draw(graphics, label, x + 12.0f, y + clipH / 2.0f - TextRenderer.height() / 2.0f, color, true);
        RenderUtils.border(graphics, x + 6.0f, y, w - 12.0f, clipH, 1.0f,
                ColorUtils.withAlpha(theme.getPrimary(), (int) (40 * alpha)));
    }

    private String keyName(int keyCode) {
        if (keyCode == -1) return "None";
        if (keyCode >= 32 && keyCode < 128) {
            String name = GLFW.glfwGetKeyName(keyCode, 0);
            if (name != null) return name.toUpperCase(Locale.ROOT);
            return String.valueOf((char) keyCode);
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT_ALT) return "LALT";
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) return "RSHIFT";
        return "KEY " + keyCode;
    }

    private void renderAppearanceTab(GuiGraphics graphics, int mouseX, int mouseY, float alpha, float delta) {
        ThemeManager theme = ThemeManager.get();
        float y = contentY + 8.0f - scroll;
        float x = contentX + 8.0f;
        float w = contentW - 16.0f;
        float bottom = y;

        bottom = renderSectionLabel(graphics, "Theme Presets", x, bottom, alpha);
        List<Theme> presets = theme.getPresets();
        float buttonW = (w - 8.0f) / 2.0f;
        for (int i = 0; i < presets.size(); i++) {
            Theme preset = presets.get(i);
            float bx = x + (i % 2) * (buttonW + 8.0f);
            float by = bottom + (i / 2) * 22.0f;
            boolean hover = mouseX >= bx && mouseX <= bx + buttonW && mouseY >= by && mouseY <= by + 18.0f;
            RenderUtils.roundedRect(graphics, bx, by, buttonW, 18.0f, 5.0f,
                    ColorUtils.withAlpha(hover ? ColorUtils.brighten(theme.getCard(), 0.08f) : theme.getBackground(), (int) (255 * alpha)));
            RenderUtils.roundedRect(graphics, bx + 3.0f, by + 4.0f, 10.0f, 10.0f, 3.0f, preset.getPrimaryColor() | 0xFF000000);
            TextRenderer.draw(graphics, presetName(i), bx + 18.0f, by + 9.0f - TextRenderer.height() / 2.0f,
                    ColorUtils.withAlpha(theme.getText(), (int) (220 * alpha)), true);
            presetHitboxes[i * 2] = bx;
            presetHitboxes[i * 2 + 1] = by;
        }
        bottom += ((presets.size() + 1) / 2) * 22.0f + 8.0f;

        bottom = renderSectionLabel(graphics, "Colors", x, bottom, alpha);
        String[] fields = {"Primary", "Secondary", "HUD Background", "Text"};
        String[] keys = {"primary", "secondary", "background", "text"};
        for (int i = 0; i < fields.length; i++) {
            float ry = bottom + i * 24.0f;
            TextRenderer.draw(graphics, fields[i], x + 4.0f, ry + 9.0f - TextRenderer.height() / 2.0f,
                    ColorUtils.withAlpha(theme.getText(), (int) (220 * alpha)), true);
            int value = theme.getThemeField(keys[i]);
            RenderUtils.roundedRect(graphics, x + w - 47.0f, ry + 1.0f, 36.0f, 18.0f, 6.0f,
                    ColorUtils.withAlpha(0xFFFFFFFF, (int) (70 * alpha)));
            RenderUtils.roundedRect(graphics, x + w - 46.0f, ry + 2.0f, 34.0f, 16.0f, 5.0f, value | 0xFF000000);
            themeSwatchY[i] = ry;
        }
        bottom += fields.length * 24.0f + 8.0f;

        bottom = renderSectionLabel(graphics, "Gradient", x, bottom, alpha);
        String mode = theme.getGradientMode().name();
        boolean hoverMode = mouseX >= x + 4 && mouseX <= x + w - 4 && mouseY >= bottom && mouseY <= bottom + 20.0f;
        RenderUtils.roundedRect(graphics, x + 4.0f, bottom, w - 8.0f, 20.0f, 5.0f,
                ColorUtils.withAlpha(hoverMode ? ColorUtils.brighten(theme.getCard(), 0.08f) : theme.getBackground(), (int) (255 * alpha)));
        TextRenderer.draw(graphics, "Mode: " + mode, x + 10.0f, bottom + 10.0f - TextRenderer.height() / 2.0f,
                ColorUtils.withAlpha(theme.getText(), (int) (230 * alpha)), true);
        gradientModeY = bottom;
        bottom += 24.0f;

        Slider speedSlider = sliderCache.computeIfAbsent(gradientSpeedSetting, k -> new Slider());
        if (!speedSlider.isDragging()) {
            gradientSpeedSetting.set(theme.getGradientSpeed());
        }
        speedSlider.render(graphics, x + 4.0f, bottom + 12.0f, w - 8.0f, 7.0f, gradientSpeedSetting, alpha);
        appearanceSliderGeom.put(gradientSpeedSetting, new float[]{x + 4.0f, bottom + 12.0f, w - 8.0f, 7.0f});
        if (gradientSpeedSetting.get() != theme.getGradientSpeed()) {
            theme.setGradientSpeed(gradientSpeedSetting.getFloat());
        }
        bottom += 30.0f;

        bottom = renderSectionLabel(graphics, "Font", x, bottom, alpha);
        List<String> ids = fontIds();
        String current = ids.get(Math.min(fontIndex, ids.size() - 1));
        boolean hoverFont = mouseX >= x + 4 && mouseX <= x + w - 4 && mouseY >= bottom && mouseY <= bottom + 20.0f;
        RenderUtils.roundedRect(graphics, x + 4.0f, bottom, w - 8.0f, 20.0f, 5.0f,
                ColorUtils.withAlpha(hoverFont ? ColorUtils.brighten(theme.getCard(), 0.08f) : theme.getBackground(), (int) (255 * alpha)));
        String fontLabel = FontManager.VANILLA_ID.equals(current)
                ? "Vanilla Minecraft" : FontManager.get().getFontDisplayName(current);
        TextRenderer.draw(graphics, "Font: " + fontLabel, x + 10.0f, bottom + 10.0f - TextRenderer.height() / 2.0f,
                ColorUtils.withAlpha(theme.getText(), (int) (230 * alpha)), true);
        fontSelectorY = bottom;
        bottom += 24.0f;

        Slider fontSlider = sliderCache.computeIfAbsent(fontScaleSetting, k -> new Slider());
        if (!fontSlider.isDragging()) {
            fontScaleSetting.set(FontManager.get().getFontScale());
        }
        fontSlider.render(graphics, x + 4.0f, bottom + 12.0f, w - 8.0f, 7.0f, fontScaleSetting, alpha);
        appearanceSliderGeom.put(fontScaleSetting, new float[]{x + 4.0f, bottom + 12.0f, w - 8.0f, 7.0f});
        if (fontScaleSetting.get() != FontManager.get().getFontScale()) {
            FontManager.get().setFontScale(fontScaleSetting.getFloat());
        }
        bottom += 30.0f;

        bottom = renderSectionLabel(graphics, "Interface", x, bottom, alpha);
        Slider opacitySlider = sliderCache.computeIfAbsent(guiOpacitySetting, k -> new Slider());
        if (!opacitySlider.isDragging()) {
            guiOpacitySetting.set(theme.getGuiOpacity());
        }
        opacitySlider.render(graphics, x + 4.0f, bottom + 12.0f, w - 8.0f, 7.0f, guiOpacitySetting, alpha);
        appearanceSliderGeom.put(guiOpacitySetting, new float[]{x + 4.0f, bottom + 12.0f, w - 8.0f, 7.0f});
        if (guiOpacitySetting.get() != theme.getGuiOpacity()) {
            theme.setGuiOpacity(guiOpacitySetting.getFloat());
        }
        bottom += 30.0f;
        Slider scaleSlider = sliderCache.computeIfAbsent(guiScaleSetting, k -> new Slider());
        if (!scaleSlider.isDragging()) {
            guiScaleSetting.set(theme.getGuiScale());
        }
        scaleSlider.render(graphics, x + 4.0f, bottom + 12.0f, w - 8.0f, 7.0f, guiScaleSetting, alpha);
        appearanceSliderGeom.put(guiScaleSetting, new float[]{x + 4.0f, bottom + 12.0f, w - 8.0f, 7.0f});
        if (guiScaleSetting.get() != theme.getGuiScale()) {
            theme.setGuiScale(guiScaleSetting.getFloat());
        }
        bottom += 30.0f;

        bottom = renderSectionLabel(graphics, "Crosshair Preview", x, bottom, alpha);
        float previewW = w - 8.0f;
        RenderUtils.roundedRect(graphics, x + 4.0f, bottom, previewW, 46.0f, 6.0f,
                ColorUtils.withAlpha(theme.getBackground(), (int) (255 * alpha)));
        CrosshairModule crosshair = CrosshairModule.get();
        if (crosshair != null) {
            crosshair.renderPreview(graphics, (int) (x + 4.0f + previewW / 2.0f), (int) (bottom + 23.0f));
        }
        crosshairPreviewY = bottom;
        bottom += 54.0f;

        maxScroll = Math.max(0.0f, bottom - contentY - contentH);
        scrollTarget = Math.max(0.0f, Math.min(maxScroll, scrollTarget));
    }

    private String presetName(int index) {
        return switch (index) {
            case 0 -> "Lumen Pink & Black";
            case 1 -> "Midnight Rose";
            case 2 -> "Cyber Magenta";
            case 3 -> "Obsidian White";
            case 4 -> "Lavender Dusk";
            default -> "Custom " + index;
        };
    }

    private float renderSectionLabel(GuiGraphics graphics, String label, float x, float y, float alpha) {
        ThemeManager theme = ThemeManager.get();
        TextRenderer.drawScaled(graphics, label, x + 4.0f, y, theme.gradientColor(0.3f, alpha), 1.0f, true);
        RenderUtils.verticalGradient(graphics, x + 4.0f, y + 13.0f, contentW - 24.0f, 1.0f,
                ColorUtils.withAlpha(theme.getPrimary(), (int) (90 * alpha)), 0x00000000);
        return y + 18.0f;
    }

    private void renderProfilesTab(GuiGraphics graphics, int mouseX, int mouseY, float alpha, float delta) {
        ThemeManager theme = ThemeManager.get();
        float y = contentY + 8.0f - scroll;
        float x = contentX + 8.0f;
        float w = contentW - 16.0f;

        TextRenderer.drawScaled(graphics, "Configuration Profiles", x + 4.0f, y, theme.gradientColor(0.3f, alpha), 1.0f, true);
        y += 20.0f;

        ConfigManager config = LumenClient.get().getConfigManager();
        profileBox.setX((int) (x + 4.0f));
        profileBox.setY((int) y);
        profileBox.setWidth((int) (w - 70.0f));
        profileBox.render(graphics, mouseX, mouseY, delta);

        float saveX = x + w - 60.0f;
        boolean hoverSave = mouseX >= saveX && mouseX <= saveX + 56.0f && mouseY >= y && mouseY <= y + 16.0f;
        RenderUtils.roundedGradient(graphics, saveX, y, 56.0f, 16.0f, 5.0f,
                theme.gradientColor(0.0f, alpha * (hoverSave ? 1.0f : 0.8f)),
                theme.gradientColor(1.0f, alpha * (hoverSave ? 1.0f : 0.8f)), false);
        TextRenderer.draw(graphics, "Save", saveX + 28.0f - TextRenderer.width("Save") / 2.0f, y + 8.0f - TextRenderer.height() / 2.0f,
                ColorUtils.withAlpha(0xFFFFFFFF, (int) (255 * alpha)), true);
        saveButtonY = y;
        y += 24.0f;

        cachedProfiles = config.listProfiles();
        profileRowYs.clear();
        for (int i = 0; i < cachedProfiles.size(); i++) {
            String name = cachedProfiles.get(i);
            boolean hover = mouseX >= x + 4 && mouseX <= x + w - 4 && mouseY >= y && mouseY <= y + 22.0f;
            RenderUtils.roundedRect(graphics, x + 4.0f, y, w - 8.0f, 22.0f, 5.0f,
                    ColorUtils.withAlpha(hover ? ColorUtils.brighten(theme.getCard(), 0.08f) : theme.getBackground(), (int) (255 * alpha)));
            TextRenderer.draw(graphics, name, x + 12.0f, y + 11.0f - TextRenderer.height() / 2.0f,
                    ColorUtils.withAlpha(theme.getText(), (int) (220 * alpha)), true);
            TextRenderer.draw(graphics, "Load", x + w - 84.0f, y + 11.0f - TextRenderer.height() / 2.0f,
                    ColorUtils.withAlpha(theme.getPrimary(), (int) (230 * alpha)), true);
            TextRenderer.draw(graphics, "Delete", x + w - 44.0f, y + 11.0f - TextRenderer.height() / 2.0f,
                    ColorUtils.withAlpha(0xFFFF6B6B, (int) (220 * alpha)), true);
            profileRowYs.add(y);
            y += 26.0f;
        }

        maxScroll = Math.max(0.0f, y + 8.0f - contentY - contentH);
        scrollTarget = Math.max(0.0f, Math.min(maxScroll, scrollTarget));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mx = toLocalX(event.x());
        double my = toLocalY(event.y());
        if (colorPicker.isOpen()) {
            if (colorPicker.mouseClicked(new MouseButtonEvent(mx, my, event.buttonInfo()), doubled)) return true;
        }
        if (event.button() != 0) return true;

        if (mx >= closeX - 6 && mx <= closeX + 12 && my >= closeY && my <= closeY + HEADER_H) {
            startClosing();
            return true;
        }

        float winX = (width - WIN_W) / 2.0f;
        float winY = (height - WIN_H) / 2.0f;
        float sidebarY = winY + HEADER_H;
        if (mx >= winX + 4 && mx <= winX + SIDEBAR_W - 4 && my >= sidebarY && my <= sidebarY + TABS.length * TAB_H) {
            int tab = (int) ((my - sidebarY - 6.0f) / TAB_H);
            if (tab >= 0 && tab < TABS.length && tab != activeTab) {
                activeTab = tab;
                expanded = null;
                waitingKeybind = null;
                scroll = 0.0f;
                scrollTarget = 0.0f;
            }
            return true;
        }
        if (hudEditorSidebarY > 0 && mx >= winX + 8 && mx <= winX + SIDEBAR_W - 8
                && my >= hudEditorSidebarY && my <= hudEditorSidebarY + 22.0f) {
            minecraft.setScreen(new HudEditorScreen(this));
            return true;
        }

        boolean inContent = mx >= contentX && mx <= contentX + contentW && my >= contentY && my <= contentY + contentH;
        if (!inContent) {
            searchFocused = false;
            return true;
        }

        if (activeTab <= 3) {
            handleModuleTabClick(mx, my);
        } else if (activeTab == 4) {
            handleAppearanceClick(mx, my);
        } else {
            handleProfilesClick(mx, my);
        }
        return true;
    }

    private void handleAppearanceSliderDown(double mx, double my) {
        for (Map.Entry<NumberSetting, float[]> entry : appearanceSliderGeom.entrySet()) {
            float[] g = entry.getValue();
            Slider slider = sliderCache.computeIfAbsent(entry.getKey(), k -> new Slider());
            if (slider.mouseDown(mx, my, entry.getKey(), g[0], g[1], g[2], g[3])) {
                return;
            }
        }
    }

    private void handleModuleTabClick(double mx, double my) {
        float rowX = contentX + 8.0f;
        float rowW = contentW - 16.0f;
        float y = contentY + 26.0f - scroll;

        if (my >= contentY + 6.0f && my <= contentY + 22.0f) {
            searchFocused = true;
            return;
        }
        searchFocused = false;

        for (Module module : filteredModules()) {
            float rowHeight = ROW_H;
            if (module == expanded) {
                rowHeight += expandAnim.getValue() * settingsHeight(module);
            }
            if (my >= y && my <= y + rowHeight) {
                if (module == expanded && my > y + ROW_H) {
                    handleSettingsClick(module, mx, my, my - y - ROW_H, rowX, rowW);
                } else if (mx >= rowX + rowW - 46.0f && mx <= rowX + rowW - 6.0f) {
                    dev.lumen.client.sfx.ClientSounds.click();
                    module.toggle();
                } else {
                    expanded = expanded == module ? null : module;
                    expandAnim.setTarget(expanded == module);
                }
                return;
            }
            y += rowHeight;
        }

        if (activeTab == 3 && gcButtonY > 0 && my >= gcButtonY && my <= gcButtonY + 20.0f) {
            SystemOptimizerModule system = SystemOptimizerModule.get();
            if (system != null) system.triggerCleanup();
        }
    }

    private void handleSettingsClick(Module module, double mx, double myAbsolute, double myRelative, float rowX, float rowW) {
        float y = 0.0f;
        for (Setting setting : module.getSettings()) {
            if (!setting.isVisible()) continue;
            if (myRelative >= y && myRelative < y + SETTING_H) {
                if (setting instanceof BooleanSetting bool) {
                    bool.toggle();
                    if (bool.get()) {
                        dev.lumen.client.sfx.ClientSounds.toggleOn();
                    } else {
                        dev.lumen.client.sfx.ClientSounds.toggleOff();
                    }
                    LumenClient.get().getConfigManager().markDirty();
                } else if (setting instanceof EnumSetting<?> enumSetting) {
                    enumSetting.cycle();
                    dev.lumen.client.sfx.ClientSounds.click();
                    LumenClient.get().getConfigManager().markDirty();
                } else if (setting instanceof NumberSetting number) {
                    Slider slider = sliderCache.computeIfAbsent(number, k -> new Slider());
                    float[] g = sliderGeom.get(number);
                    if (g != null) {
                        slider.mouseDown(mx, myAbsolute, number, g[0], g[1], g[2], g[3]);
                    }
                    LumenClient.get().getConfigManager().markDirty();
                } else if (setting instanceof ColorSetting color) {
                    float pickerX = rowX + rowW - ColorPickerPanel.WIDTH - 8.0f;
                    colorPicker.open(color::getRaw, color::set, pickerX, contentY + 20.0f);
                } else if (setting instanceof FontSetting font) {
                    List<String> ids = fontIds();
                    int index = ids.indexOf(font.get());
                    font.set(ids.get(Math.max(0, (index + 1) % ids.size())));
                    LumenClient.get().getConfigManager().markDirty();
                }
                return;
            }
            y += SETTING_H;
        }
        if (myRelative >= y && myRelative < y + SETTING_H) {
            waitingKeybind = waitingKeybind == module ? null : module;
        }
    }

    private void handleAppearanceClick(double mx, double my) {
        handleAppearanceSliderDown(mx, my);
        ThemeManager theme = ThemeManager.get();
        List<Theme> presets = theme.getPresets();
        for (int i = 0; i < presets.size(); i++) {
            float bx = presetHitboxes[i * 2];
            float by = presetHitboxes[i * 2 + 1];
            if (bx > 0 && mx >= bx && mx <= bx + (contentW - 16.0f) / 2.0f && my >= by && my <= by + 18.0f) {
                Theme source = presets.get(i);
                Theme copy = new Theme();
                copy.setPrimaryColor(source.getPrimaryColor());
                copy.setSecondaryColor(source.getSecondaryColor());
                copy.setBackgroundColor(source.getBackgroundColor());
                copy.setCardColor(source.getCardColor());
                copy.setTextColor(source.getTextColor());
                copy.setGradientMode(source.getGradientMode());
                copy.setGradientSpeed(source.getGradientSpeed());
                theme.applyTheme(copy);
                return;
            }
        }

        String[] keys = {"primary", "secondary", "background", "text"};
        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            float ry = themeSwatchY[i];
            if (ry > 0 && mx >= contentX + contentW - 54.0f && mx <= contentX + contentW - 14.0f
                    && my >= ry && my <= ry + 20.0f) {
                float pickerX = contentX + contentW - ColorPickerPanel.WIDTH - 10.0f;
                colorPicker.open(() -> theme.getThemeField(key), v -> theme.setThemeField(key, v),
                        pickerX, contentY + 20.0f);
                return;
            }
        }

        if (gradientModeY > 0 && my >= gradientModeY && my <= gradientModeY + 20.0f) {
            ThemeManager.GradientMode[] modes = ThemeManager.GradientMode.values();
            theme.setGradientMode(modes[(theme.getGradientMode().ordinal() + 1) % modes.length]);
            return;
        }
        if (fontSelectorY > 0 && my >= fontSelectorY && my <= fontSelectorY + 20.0f) {
            List<String> ids = fontIds();
            fontIndex = (fontIndex + 1) % ids.size();
            FontManager.get().selectFont(ids.get(fontIndex));
            FontManager.get().ensureActiveRenderer();
            LumenClient.get().getConfigManager().markDirty();
        }
    }

    private void handleProfilesClick(double mx, double my) {
        ConfigManager config = LumenClient.get().getConfigManager();
        if (saveButtonY > 0 && my >= saveButtonY && my <= saveButtonY + 16.0f
                && mx >= contentX + contentW - 68.0f && mx <= contentX + contentW - 8.0f) {
            String name = profileBox.getValue().trim();
            if (!name.isEmpty()) {
                config.saveProfile(name);
                profileBox.setValue("");
            }
            return;
        }
        for (int i = 0; i < profileRowYs.size(); i++) {
            float ry = profileRowYs.get(i);
            if (my >= ry && my <= ry + 22.0f) {
                String name = cachedProfiles.get(i);
                if (mx >= contentX + contentW - 52.0f) {
                    config.deleteProfile(name);
                } else if (mx >= contentX + contentW - 92.0f) {
                    config.loadProfile(name);
                }
                return;
            }
        }
        if (profileBox.isMouseOver(mx, my)) {
            profileBox.setFocused(true);
        } else {
            profileBox.setFocused(false);
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double lx = toLocalX(event.x());
        double ly = toLocalY(event.y());
        if (colorPicker.isOpen() && colorPicker.mouseDragged(new MouseButtonEvent(lx, ly, event.buttonInfo()), dragX, dragY)) {
            return true;
        }
        NumberSetting owner = activeSliderOwner();
        if (owner != null) {
            float[] g = sliderGeom.get(owner);
            if (g == null) g = appearanceSliderGeom.get(owner);
            if (g != null) {
                Slider slider = sliderCache.get(owner);
                slider.drag(lx, owner, g[0], g[2]);
                return true;
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    private NumberSetting activeSliderOwner() {
        for (Map.Entry<Object, Slider> entry : sliderCache.entrySet()) {
            if (entry.getValue().isDragging() && entry.getKey() instanceof NumberSetting setting) {
                return setting;
            }
        }
        return null;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        colorPicker.mouseReleased();
        for (Slider slider : sliderCache.values()) {
            slider.release();
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollTarget = Math.max(0.0f, Math.min(maxScroll, scrollTarget - (float) scrollY * 26.0f));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (waitingKeybind != null) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                waitingKeybind.setKeyCode(-1);
            } else if (event.key() != GLFW.GLFW_KEY_UNKNOWN) {
                waitingKeybind.setKeyCode(event.key());
            }
            waitingKeybind = null;
            return true;
        }
        if (colorPicker.keyPressed(event)) return true;

        if (searchFocused) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
            }
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            startClosing();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (colorPicker.charTyped(event)) return true;
        if (searchFocused) {
            String text = event.codepointAsString();
            if (text.length() == 1 && text.charAt(0) >= 32 && text.charAt(0) != 127) {
                search += text;
                return true;
            }
        }
        if (profileBox != null && profileBox.isFocused()) {
            return profileBox.charTyped(event);
        }
        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
