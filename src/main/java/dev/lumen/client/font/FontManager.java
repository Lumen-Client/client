package dev.lumen.client.font;

import dev.lumen.client.LumenClient;
import dev.lumen.client.render.LumenFontRenderer;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads bundled and user-provided TTF fonts, selects the active one and
 * exposes {@link LumenFontRenderer} instances. Custom fonts placed in
 * .minecraft/config/lumen/fonts/ are picked up at startup.
 */
public class FontManager {
    public static final String VANILLA_ID = "vanilla";

    private static FontManager instance;

    private final Path customFontDir;
    private final Map<String, FontEntry> fonts = new LinkedHashMap<>();
    private String selectedFontId = "inter";
    private boolean vanillaFont;
    private float fontScale = 1.0f;
    private boolean shadow = true;

    public FontManager(Path customFontDir) {
        instance = this;
        this.customFontDir = customFontDir;
        registerBundled("inter", "Inter", "/assets/lumen/fonts/Inter-Medium.ttf");
        registerBundled("poppins", "Poppins", "/assets/lumen/fonts/Poppins-SemiBold.ttf");
        registerBundled("jetbrainsmono", "JetBrains Mono", "/assets/lumen/fonts/JetBrainsMono-Medium.ttf");
    }

    public static FontManager get() {
        return LumenClient.get().getFontManager();
    }

    private void registerBundled(String id, String name, String resourcePath) {
        try (InputStream stream = FontManager.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                LumenClient.LOGGER.warn("Bundled font missing: {}", resourcePath);
                return;
            }
            byte[] bytes = stream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes).flip();
            fonts.put(id, new FontEntry(id, name, buffer, null));
        } catch (IOException e) {
            LumenClient.LOGGER.error("Failed to load bundled font {}", resourcePath, e);
        }
    }

    public void loadFonts() {
        if (customFontDir == null || !Files.isDirectory(customFontDir)) {
            return;
        }
        try (Stream<Path> stream = Files.list(customFontDir)) {
            stream.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".ttf"))
                    .forEach(this::registerCustom);
        } catch (IOException e) {
            LumenClient.LOGGER.error("Failed to scan custom fonts", e);
        }
    }

    private void registerCustom(Path path) {
        String id = path.getFileName().toString().replace(".ttf", "").replace(".TTF", "");
        try {
            byte[] bytes = Files.readAllBytes(path);
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes).flip();
            fonts.put(id, new FontEntry(id, id, buffer, path));
            LumenClient.LOGGER.info("Loaded custom font: {}", id);
        } catch (IOException e) {
            LumenClient.LOGGER.error("Failed to load custom font {}", path, e);
        }
    }

    public List<String> getFontIds() {
        return new ArrayList<>(fonts.keySet());
    }

    public String getFontDisplayName(String id) {
        FontEntry entry = fonts.get(id);
        return entry == null ? id : entry.displayName;
    }

    public String getSelectedFontId() {
        return selectedFontId;
    }

    public void selectFont(String id) {
        if (fonts.containsKey(id) || VANILLA_ID.equals(id)) {
            this.selectedFontId = id;
            this.vanillaFont = VANILLA_ID.equals(id);
        }
    }

    public boolean isVanillaFont() {
        return vanillaFont;
    }

    public void setVanillaFont(boolean vanilla) {
        this.vanillaFont = vanilla;
        if (vanilla) {
            this.selectedFontId = VANILLA_ID;
        } else if (VANILLA_ID.equals(selectedFontId)) {
            this.selectedFontId = fonts.containsKey("inter") ? "inter" : fonts.keySet().iterator().next();
        }
    }

    public float getFontScale() {
        return fontScale;
    }

    public void setFontScale(float scale) {
        this.fontScale = scale;
    }

    public boolean hasShadow() {
        return shadow;
    }

    public void setShadow(boolean shadow) {
        this.shadow = shadow;
    }

    public void onFontSettingChanged() {
        // hook for future live reloads
    }

    /**
     * Returns the active custom renderer, or null when the vanilla font is
     * selected or the font could not be loaded.
     */
    public LumenFontRenderer getActiveRenderer() {
        if (vanillaFont) return null;
        FontEntry entry = fonts.get(selectedFontId);
        if (entry == null) {
            entry = fonts.get("inter");
            if (entry == null) return null;
        }
        return entry.renderer;
    }

    public LumenFontRenderer getRenderer(String id) {
        FontEntry entry = fonts.get(id);
        return entry == null ? null : entry.renderer;
    }

    public void ensureRenderer(String id) {
        FontEntry entry = fonts.get(id);
        if (entry != null && entry.renderer == null) {
            entry.renderer = new LumenFontRenderer(id, entry.data);
        }
    }

    public void ensureActiveRenderer() {
        if (vanillaFont) return;
        String id = fonts.containsKey(selectedFontId) ? selectedFontId : "inter";
        ensureRenderer(id);
    }

    private static final class FontEntry {
        final String id;
        final String displayName;
        final ByteBuffer data;
        final Path sourcePath;
        LumenFontRenderer renderer;

        FontEntry(String id, String displayName, ByteBuffer data, Path sourcePath) {
            this.id = id;
            this.displayName = displayName;
            this.data = data;
            this.sourcePath = sourcePath;
        }
    }
}
