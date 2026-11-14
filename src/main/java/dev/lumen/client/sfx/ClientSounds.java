package dev.lumen.client.sfx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Subtle UI sound effects for the client. All sounds are played through the
 * UI sound channel (no positional audio) at low volume so they stay clean
 * and non-intrusive. Every call is a no-op when the sound manager is not
 * ready (e.g. during startup).
 */
public final class ClientSounds {
    private static final float VOLUME = 0.18f;

    private ClientSounds() {
    }

    private static void ui(SoundEvent sound, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getSoundManager() == null) return;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, VOLUME));
    }

    public static void guiOpen() {
        ui(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.6f);
    }

    public static void guiClose() {
        ui(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.2f);
    }

    public static void click() {
        ui(SoundEvents.UI_BUTTON_CLICK.value(), 1.5f);
    }

    public static void toggleOn() {
        ui(SoundEvents.NOTE_BLOCK_HAT.value(), 1.8f);
    }

    public static void toggleOff() {
        ui(SoundEvents.NOTE_BLOCK_HAT.value(), 1.3f);
    }

    public static void sliderTick() {
        ui(SoundEvents.NOTE_BLOCK_HAT.value(), 2.0f);
    }

    public static void moduleEnable() {
        ui(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.7f);
    }

    public static void moduleDisable() {
        ui(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.1f);
    }

    public static void snap() {
        ui(SoundEvents.NOTE_BLOCK_BIT.value(), 1.9f);
    }
}
