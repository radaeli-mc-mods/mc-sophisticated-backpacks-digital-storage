package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import net.minecraft.world.item.ItemStack;

/** Log only routing metadata: no contents, component payloads, or cached network authority. */
public final class DeliveryDiagnostics {
    private DeliveryDiagnostics() {}
    public static String endpoint(ItemStack upgrade) {
        return EndpointBinding.read(upgrade).map(value -> "backend=" + value.backendId()
                + ", endpoint=" + value.dimension() + "@" + value.pos().toShortString())
                .orElse("backend=local, endpoint=none");
    }
    public static String describe(ItemStack upgrade, String stage) { return endpoint(upgrade) + ", stage=" + stage; }
}
