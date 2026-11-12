package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Shared presentation used by item tooltips, tabs and server feedback. */
public final class StorageText {
    private StorageText() {
    }

    public static Component backendName(String backendId) {
        return Component.translatableWithFallback("backend.sophisticated_backpacks_digital_storages_expansion." + backendId, backendId);
    }

    public static Component endpoint(String translationKey, StorageEndpoint endpoint) {
        return Component.translatable(translationKey, backendName(endpoint.backendId()),
                endpoint.dimension().location().toString(), endpoint.pos().getX(), endpoint.pos().getY(), endpoint.pos().getZ());
    }

    public static Component summary(StorageEndpoint endpoint) {
        return Component.translatable("gui.sophisticated_backpacks_digital_storages_expansion.linked",
                shortBackendName(endpoint.backendId()));
    }

    public static Component tooltipSummary(StorageEndpoint endpoint) {
        return Component.translatable("gui.sophisticated_backpacks_digital_storages_expansion.linked",
                shortBackendName(endpoint.backendId()).withStyle(ChatFormatting.GREEN))
                .withStyle(ChatFormatting.DARK_GRAY);
    }

    private static MutableComponent shortBackendName(String backendId) {
        return Component.translatableWithFallback("backend.sophisticated_backpacks_digital_storages_expansion." + backendId + ".short",
                backendId);
    }
}
