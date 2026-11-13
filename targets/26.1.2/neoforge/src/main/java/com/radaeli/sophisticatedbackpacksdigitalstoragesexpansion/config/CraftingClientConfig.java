package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting.ManualControlsPreference;

/** Client-only configuration shared by the crafting tab and its explicit toggle. */
public final class CraftingClientConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.EnumValue<ManualControlsPreference> MANUAL_CONTROLS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("crafting");
        MANUAL_CONTROLS = builder.comment("Controls when the recipe selector and Fetch ingredients button are shown.")
                .defineEnum("manualControls", ManualControlsPreference.AUTO);
        builder.pop();
        SPEC = builder.build();
    }

    private CraftingClientConfig() {
    }

    public static ManualControlsPreference manualControls() {
        return MANUAL_CONTROLS.get();
    }

    public static void setManualControls(ManualControlsPreference preference) {
        MANUAL_CONTROLS.set(preference);
        SPEC.save();
    }
}
