package dev.lumen.client.modules.visual;

import dev.lumen.client.core.Module;
import dev.lumen.client.core.Category;

public class NoHurtCamModule extends Module {
    private static NoHurtCamModule instance;

    public NoHurtCamModule() {
        super("No Hurt Cam", "Removes the shake effect when damaged", Category.VISUALS);
        instance = this;
    }

    public static NoHurtCamModule get() {
        return instance;
    }

    public boolean shouldCancel() {
        return isEnabled();
    }
}
