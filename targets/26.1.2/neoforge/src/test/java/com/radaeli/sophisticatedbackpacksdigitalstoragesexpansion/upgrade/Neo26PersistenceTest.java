package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.MinecraftStorageTest;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Neo26PersistenceTest extends MinecraftStorageTest {
    private static RegistryAccess registries() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }
    @Test void serializedPendingItemsKeepComponentsAndQuarantineUntilExplicitRecovery() {
        var upgrade = new ItemStack(com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.ModItems.REMOTE_PICKUP_UPGRADE.get());
        var pending = new ItemStack(Items.DIAMOND, 7);
        pending.set(DataComponents.CUSTOM_NAME, Component.literal("reserved components"));
        PickupReservation.write(upgrade, pending, registries());
        PickupReservation.markUncertain(upgrade);
        var encoded = ItemStack.CODEC.encodeStart(net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, registries()), upgrade).getOrThrow();
        var restored = ItemStack.CODEC.parse(net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, registries()), encoded).getOrThrow();
        assertTrue(PickupReservation.hasPending(restored));
        assertTrue(PickupReservation.uncertain(restored));
        assertTrue(ItemStack.matches(pending, PickupReservation.read(restored, registries())));
        assertFalse(((RemotePickupUpgradeItem) restored.getItem()).canChangeBinding(restored));
        PickupReservation.write(restored, ItemStack.EMPTY, registries());
        assertFalse(PickupReservation.hasPending(restored));
        assertFalse(PickupReservation.uncertain(restored));
    }
    @Test void aKnownRemainderSettlesThePersistedInFlightPhase() {
        var upgrade = new ItemStack(Items.PAPER);
        PickupReservation.write(upgrade, new ItemStack(Items.DIAMOND, 7), registries());
        PickupReservation.markUncertain(upgrade);
        PickupReservation.write(upgrade, new ItemStack(Items.DIAMOND, 3), registries());
        assertFalse(PickupReservation.uncertain(upgrade));
        assertTrue(PickupReservation.hasPending(upgrade));
        assertEquals(3, PickupReservation.read(upgrade, registries()).getCount());
    }
    @Test void catalogRoundTripPreservesSessionAndResultComponents() {
        var result = new ItemStack(Items.PAPER, 3);
        result.set(DataComponents.CUSTOM_NAME, Component.literal("result components"));
        var payload = new CraftingCatalogPayload(4, 2, 128L, true,
                List.of(new CraftingCatalogPayload.Entry(Identifier.parse("minecraft:paper"), result)));
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries());
        try {
            CraftingCatalogPayload.CODEC.encode(buffer, payload);
            var decoded = CraftingCatalogPayload.CODEC.decode(buffer);
            assertEquals(payload.menu(), decoded.menu()); assertEquals(payload.container(), decoded.container());
            assertEquals(payload.request(), decoded.request()); assertTrue(decoded.first());
            assertEquals(payload.entries().getFirst().id(), decoded.entries().getFirst().id());
            assertTrue(ItemStack.matches(result, decoded.entries().getFirst().result()));
        } finally { buffer.release(); }
    }
    @Test void unlinkRecipeFromDataCanBeEncodedForClients() {
        var codec = com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.recipe.ModRecipes.UNLINK_UPGRADE.get();
        var recipe = codec.codec().codec().parse(com.mojang.serialization.JsonOps.INSTANCE,
                new com.google.gson.JsonObject()).getOrThrow();
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries());
        try {
            codec.streamCodec().encode(buffer, recipe);
            assertInstanceOf(com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.recipe.UnlinkUpgradeRecipe.class,
                    codec.streamCodec().decode(buffer));
        } finally { buffer.release(); }
    }
    @Test void catalogRejectsOversizedIncomingPages() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries());
        try {
            buffer.writeVarInt(1); buffer.writeVarInt(0); buffer.writeLong(1); buffer.writeBoolean(true);
            buffer.writeVarInt(CraftingCatalogPayload.PAGE_SIZE + 1);
            assertThrows(IllegalArgumentException.class, () -> CraftingCatalogPayload.CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
}


