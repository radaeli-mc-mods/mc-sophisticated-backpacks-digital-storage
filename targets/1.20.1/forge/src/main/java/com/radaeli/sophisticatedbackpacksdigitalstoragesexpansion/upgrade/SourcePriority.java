package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.util.StringRepresentable;

/** Ordering used when an automated upgrade can use both the linked network and the backpack. */
public enum SourcePriority implements StringRepresentable {
    NETWORK_FIRST("network_first"),
    BACKPACK_FIRST("backpack_first");

    private final String serializedName;

    SourcePriority(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public SourcePriority next() {
        return this == NETWORK_FIRST ? BACKPACK_FIRST : NETWORK_FIRST;
    }
}
