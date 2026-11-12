package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Retains strict legacy field validation with the optional-based NBT API. */
public final class NbtChecks {
    private NbtChecks() {}
    public static boolean has(CompoundTag data, String key, int type) {
        Tag value = data.get(key);
        return value != null && value.getId() == type;
    }
}
