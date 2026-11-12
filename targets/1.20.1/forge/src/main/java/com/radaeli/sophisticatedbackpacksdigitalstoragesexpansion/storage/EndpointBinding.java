package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Optional;
import java.util.OptionalLong;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class EndpointBinding {
    private static final String LINK_TAG = "remote_storage_link";
    private static final String LINKED_AT = "linked_at";
    private static final String LINKED_FROM = "linked_from";

    private EndpointBinding() {
    }

    public static Optional<StorageEndpoint> read(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null) return Optional.empty();
        if (!root.contains(LINK_TAG, CompoundTag.TAG_COMPOUND)) return Optional.empty();
        CompoundTag link = root.getCompound(LINK_TAG);
        String backend = link.getString("backend");
        ResourceLocation dimension = ResourceLocation.tryParse(link.getString("dimension"));
        if (backend.isBlank() || dimension == null || !link.contains("pos", CompoundTag.TAG_LONG)) return Optional.empty();
        return Optional.of(new StorageEndpoint(backend, ResourceKey.create(Registries.DIMENSION, dimension),
                BlockPos.of(link.getLong("pos"))));
    }

    public static boolean hasBinding(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(LINK_TAG);
    }

    public static void write(ItemStack stack, StorageEndpoint endpoint) {
        CompoundTag link = new CompoundTag();
        link.putString("backend", endpoint.backendId());
        link.putString("dimension", endpoint.dimension().location().toString());
        link.putLong("pos", endpoint.pos().asLong());
        stack.getOrCreateTag().put(LINK_TAG, link);
    }

    /**
     * Records when a player linked the item and which backend it was linked to before, so the client
     * can play the matching link or transition animation once.
     */
    public static void markLinked(ItemStack stack, long gameTime, Optional<String> previousBackend) {
        CompoundTag link = stack.getOrCreateTag().getCompound(LINK_TAG);
        link.putLong(LINKED_AT, gameTime);
        previousBackend.ifPresent(backend -> link.putString(LINKED_FROM, backend));
        stack.getTag().put(LINK_TAG, link);
    }

    public static Optional<String> linkedFrom(ItemStack stack) {
        CompoundTag link = linkTag(stack);
        return link.contains(LINKED_FROM, CompoundTag.TAG_STRING) ? Optional.of(link.getString(LINKED_FROM)) : Optional.empty();
    }

    public static OptionalLong linkedAt(ItemStack stack) {
        CompoundTag link = linkTag(stack);
        return link.contains(LINKED_AT, CompoundTag.TAG_LONG) ? OptionalLong.of(link.getLong(LINKED_AT)) : OptionalLong.empty();
    }

    private static CompoundTag linkTag(ItemStack stack) {
        CompoundTag root = stack.getTag();
        return root == null ? new CompoundTag() : root.getCompound(LINK_TAG);
    }

    public static void clear(ItemStack stack) {
        if (stack.hasTag()) stack.getTag().remove(LINK_TAG);
    }
}
