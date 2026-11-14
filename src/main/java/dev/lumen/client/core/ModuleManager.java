package dev.lumen.client.core;

import dev.lumen.client.LumenClient;
import dev.lumen.client.core.config.ConfigManager;
import dev.lumen.client.modules.hud.*;
import dev.lumen.client.modules.perf.*;
import dev.lumen.client.modules.visual.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ModuleManager {
    private static ModuleManager instance;

    private final List<Module> modules = new ArrayList<>();
    private final Map<Category, List<Module>> byCategory = new EnumMap<>(Category.class);

    public ModuleManager() {
        instance = this;
        for (Category category : Category.values()) {
            byCategory.put(category, new ArrayList<>());
        }
        registerModules();
    }

    private void registerModules() {
        register(new FpsModule());
        register(new PingModule());
        register(new ServerInfoModule());
        register(new CoordinatesModule());
        register(new CompassModule());
        register(new KeystrokesModule());
        register(new SprintStatusModule());
        register(new PotionStatusModule());
        register(new ArmorStatusModule());
        register(new TargetHudModule());
        register(new ComboCounterModule());

        register(new CrosshairModule());
        register(new ZoomModule());
        register(new FreelookModule());
        register(new FullbrightModule());
        register(new HitboxesModule());
        register(new NoHurtCamModule());
        register(new LowFireModule());
        register(new LowShieldModule());
        register(new TimeChangerModule());


        register(new ParticleOptimizerModule());
        register(new ChunkOptimizerModule());
        register(new EntityOptimizerModule());
        register(new RenderOptimizerModule());
        register(new SystemOptimizerModule());
    }

    private void register(Module module) {
        modules.add(module);
        byCategory.get(module.getCategory()).add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getByCategory(Category category) {
        return byCategory.get(category);
    }

    public List<HudModule> getHudModules() {
        List<HudModule> result = new ArrayList<>();
        for (Module module : modules) {
            if (module instanceof HudModule hud) {
                result.add(hud);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        for (Module module : modules) {
            if (type.isInstance(module)) {
                return (T) module;
            }
        }
        return null;
    }

    public Module getByName(String name) {
        for (Module module : modules) {
            if (module.getName().equals(name)) {
                return module;
            }
        }
        return null;
    }

    public void forEach(Consumer<Module> consumer) {
        for (Module module : modules) {
            consumer.accept(module);
        }
    }

    public void tickAll() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onClientTick();
            }
        }
    }

    public void renderAll() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onRenderTick();
            }
        }
    }

    public static ModuleManager get() {
        return LumenClient.get().getModuleManager();
    }
}
