package dev.lumen.client.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lumen.client.Lumen;
import dev.lumen.client.LumenClient;
import dev.lumen.client.core.Category;
import dev.lumen.client.core.Module;
import dev.lumen.client.core.setting.*;
import dev.lumen.client.font.FontManager;
import dev.lumen.client.theme.Theme;
import dev.lumen.client.theme.ThemeManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe Gson configuration persistence under .minecraft/config/lumen/.
 */
public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path configDir;
    private final Path profilesDir;
    private final Path fontsDir;
    private final Object ioLock = new Object();
    private volatile boolean dirty;

    public ConfigManager() {
        Path root = FabricLoader.getInstance().getConfigDir();
        this.configDir = root.resolve(Lumen.CONFIG_DIR);
        this.profilesDir = configDir.resolve("profiles");
        this.fontsDir = configDir.resolve("fonts");
        try {
            Files.createDirectories(profilesDir);
            Files.createDirectories(fontsDir);
        } catch (IOException e) {
            LumenClient.LOGGER.error("Could not create Lumen config directories", e);
        }
    }

    public Path getConfigDir() {
        return configDir;
    }

    public Path getFontsDir() {
        return fontsDir;
    }

    public void markDirty() {
        dirty = true;
    }

    public void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    public void save() {
        synchronized (ioLock) {
            try {
                Files.writeString(configDir.resolve("config.json"), GSON.toJson(serialize()), StandardCharsets.UTF_8);
                dirty = false;
            } catch (IOException e) {
                LumenClient.LOGGER.error("Failed to save Lumen config", e);
            }
        }
    }

    public void load() {
        synchronized (ioLock) {
            Path file = configDir.resolve("config.json");
            if (!Files.exists(file)) return;
            try {
                JsonElement element = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
                if (element.isJsonObject()) {
                    deserialize(element.getAsJsonObject());
                }
            } catch (Exception e) {
                LumenClient.LOGGER.error("Failed to load Lumen config", e);
            }
        }
    }

    public JsonObject serialize() {
        JsonObject root = new JsonObject();
        root.add("theme", serializeTheme(ThemeManager.get().getThemeObject()));

        JsonArray customThemes = new JsonArray();
        for (Theme custom : ThemeManager.get().getCustomPresetsList()) {
            customThemes.add(serializeTheme(custom));
        }
        root.add("customThemes", customThemes);

        JsonObject modules = new JsonObject();
        ModuleManagerHolder.modules().forEach(module -> modules.add(module.getName(), serializeModule(module)));
        root.add("modules", modules);

        JsonObject font = new JsonObject();
        font.addProperty("selected", FontManager.get().getSelectedFontId());
        font.addProperty("vanilla", FontManager.get().isVanillaFont());
        font.addProperty("scale", FontManager.get().getFontScale());
        font.addProperty("shadow", FontManager.get().hasShadow());
        root.add("font", font);
        return root;
    }

    public void deserialize(JsonObject root) {
        if (root.has("theme")) {
            ThemeManager.get().setThemeObject(deserializeTheme(root.getAsJsonObject("theme")));
        }
        if (root.has("customThemes") && root.get("customThemes").isJsonArray()) {
            List<Theme> customs = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("customThemes")) {
                customs.add(deserializeTheme(element.getAsJsonObject()));
            }
            ThemeManager.get().getCustomPresetsList().addAll(customs);
        }
        if (root.has("modules") && root.get("modules").isJsonObject()) {
            JsonObject modules = root.getAsJsonObject("modules");
            ModuleManagerHolder.modules().forEach(module -> {
                if (modules.has(module.getName())) {
                    deserializeModule(module, modules.getAsJsonObject(module.getName()));
                }
            });
        }
        if (root.has("font") && root.get("font").isJsonObject()) {
            JsonObject font = root.getAsJsonObject("font");
            if (font.has("selected")) FontManager.get().selectFont(font.get("selected").getAsString());
            if (font.has("vanilla")) FontManager.get().setVanillaFont(font.get("vanilla").getAsBoolean());
            if (font.has("scale")) FontManager.get().setFontScale(font.get("scale").getAsFloat());
            if (font.has("shadow")) FontManager.get().setShadow(font.get("shadow").getAsBoolean());
        }
    }

    private JsonObject serializeTheme(Theme theme) {
        JsonObject obj = new JsonObject();
        obj.addProperty("primary", theme.getPrimaryColor());
        obj.addProperty("secondary", theme.getSecondaryColor());
        obj.addProperty("background", theme.getBackgroundColor());
        obj.addProperty("card", theme.getCardColor());
        obj.addProperty("text", theme.getTextColor());
        obj.addProperty("gradientMode", theme.getGradientMode());
        obj.addProperty("gradientSpeed", theme.getGradientSpeed());
        return obj;
    }

    private Theme deserializeTheme(JsonObject obj) {
        Theme theme = new Theme();
        if (obj.has("primary")) theme.setPrimaryColor(obj.get("primary").getAsInt());
        if (obj.has("secondary")) theme.setSecondaryColor(obj.get("secondary").getAsInt());
        if (obj.has("background")) theme.setBackgroundColor(obj.get("background").getAsInt());
        if (obj.has("card")) theme.setCardColor(obj.get("card").getAsInt());
        if (obj.has("text")) theme.setTextColor(obj.get("text").getAsInt());
        if (obj.has("gradientMode")) theme.setGradientMode(obj.get("gradientMode").getAsString());
        if (obj.has("gradientSpeed")) theme.setGradientSpeed(obj.get("gradientSpeed").getAsFloat());
        return theme;
    }

    private JsonObject serializeModule(Module module) {
        JsonObject obj = new JsonObject();
        obj.addProperty("enabled", module.isEnabled());
        obj.addProperty("key", module.getKeyCode());
        if (module instanceof dev.lumen.client.core.HudModule hud) {
            obj.addProperty("x", hud.getX());
            obj.addProperty("y", hud.getY());
        }
        JsonArray settings = new JsonArray();
        for (Setting setting : module.getSettings()) {
            JsonObject s = new JsonObject();
            s.addProperty("name", setting.getName());
            if (setting instanceof BooleanSetting bool) {
                s.addProperty("value", bool.get());
            } else if (setting instanceof NumberSetting number) {
                s.addProperty("value", number.get());
            } else if (setting instanceof ColorSetting color) {
                s.addProperty("value", color.getRaw());
                s.addProperty("syncTheme", color.isSyncWithTheme());
            } else if (setting instanceof EnumSetting<?> enumSetting) {
                s.addProperty("value", enumSetting.get().name());
            } else if (setting instanceof KeybindSetting keybind) {
                s.addProperty("value", keybind.get());
            } else if (setting instanceof FontSetting fontSetting) {
                s.addProperty("value", fontSetting.get());
            }
            settings.add(s);
        }
        obj.add("settings", settings);
        return obj;
    }

    private void deserializeModule(Module module, JsonObject obj) {
        if (obj.has("enabled")) {
            boolean enabled = obj.get("enabled").getAsBoolean();
            if (enabled != module.isEnabled()) {
                module.setEnabled(enabled);
            }
        }
        if (obj.has("key")) {
            module.setKeyCode(obj.get("key").getAsInt());
        }
        if (module instanceof dev.lumen.client.core.HudModule hud && obj.has("x") && obj.has("y")) {
            hud.setPosition(obj.get("x").getAsFloat(), obj.get("y").getAsFloat());
        }
        if (obj.has("settings") && obj.get("settings").isJsonArray()) {
            for (JsonElement element : obj.getAsJsonArray("settings")) {
                JsonObject s = element.getAsJsonObject();
                Setting setting = module.getSetting(s.get("name").getAsString(), Setting.class);
                if (setting == null || !s.has("value")) continue;
                JsonElement value = s.get("value");
                if (setting instanceof BooleanSetting bool && value.isJsonPrimitive()) {
                    bool.set(value.getAsBoolean());
                } else if (setting instanceof NumberSetting number && value.isJsonPrimitive()) {
                    number.set(value.getAsDouble());
                } else if (setting instanceof ColorSetting color && value.isJsonPrimitive()) {
                    color.set(value.getAsInt());
                    if (s.has("syncTheme")) color.setSyncWithTheme(s.get("syncTheme").getAsBoolean());
                } else if (setting instanceof EnumSetting<?> enumSetting && value.isJsonPrimitive()) {
                    for (Enum<?> constant : enumSetting.getConstants()) {
                        if (constant.name().equals(value.getAsString())) {
                            setEnumValue(enumSetting, constant);
                            break;
                        }
                    }
                } else if (setting instanceof KeybindSetting keybind && value.isJsonPrimitive()) {
                    keybind.set(value.getAsInt());
                } else if (setting instanceof FontSetting fontSetting && value.isJsonPrimitive()) {
                    fontSetting.set(value.getAsString());
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setEnumValue(EnumSetting setting, Enum constant) {
        setting.set(constant);
    }

    public List<String> listProfiles() {
        List<String> names = new ArrayList<>();
        if (!Files.exists(profilesDir)) return names;
        try (var stream = Files.list(profilesDir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(p -> names.add(p.getFileName().toString().replace(".json", "")));
        } catch (IOException e) {
            LumenClient.LOGGER.error("Failed to list profiles", e);
        }
        return names;
    }

    public void saveProfile(String name) {
        synchronized (ioLock) {
            try {
                Path safe = profilesDir.resolve(sanitize(name) + ".json");
                Files.writeString(safe, GSON.toJson(serialize()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                LumenClient.LOGGER.error("Failed to save profile", e);
            }
        }
    }

    public void loadProfile(String name) {
        synchronized (ioLock) {
            Path file = profilesDir.resolve(sanitize(name) + ".json");
            if (!Files.exists(file)) return;
            try {
                JsonElement element = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
                if (element.isJsonObject()) {
                    deserialize(element.getAsJsonObject());
                }
            } catch (Exception e) {
                LumenClient.LOGGER.error("Failed to load profile", e);
            }
        }
    }

    public void deleteProfile(String name) {
        try {
            Files.deleteIfExists(profilesDir.resolve(sanitize(name) + ".json"));
        } catch (IOException e) {
            LumenClient.LOGGER.error("Failed to delete profile", e);
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    /** Indirection to avoid a circular constructor dependency. */
    private static final class ModuleManagerHolder {
        static dev.lumen.client.core.ModuleManager modules() {
            return LumenClient.get().getModuleManager();
        }
    }
}
