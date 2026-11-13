package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.config;

import net.minecraftforge.common.ForgeConfigSpec;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting.ManualControlsPreference;

/** Client-only configuration shared by the crafting tab and its explicit toggle. */
public final class CraftingClientConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.EnumValue<ManualControlsPreference> MANUAL_CONTROLS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
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
