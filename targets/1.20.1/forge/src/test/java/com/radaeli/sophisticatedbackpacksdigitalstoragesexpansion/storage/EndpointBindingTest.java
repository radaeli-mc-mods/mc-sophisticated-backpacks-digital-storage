package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EndpointBindingTest extends MinecraftStorageTest {
    @Test
    void keepsExistingBindingFormatAndPreservesUnrelatedData() {
        ItemStack stack = new ItemStack(Items.STICK);
        stack.getOrCreateTag().putString("other_addon", "keep");
        StorageEndpoint endpoint = new StorageEndpoint("refinedstorage", Level.NETHER, new BlockPos(12, 64, -9));
        EndpointBinding.write(stack, endpoint);
        assertEquals(endpoint, EndpointBinding.read(stack.copy()).orElseThrow());
        assertEquals("refinedstorage", stack.getTag().getCompound("remote_storage_link").getString("backend"));
        assertEquals("minecraft:the_nether", stack.getTag().getCompound("remote_storage_link").getString("dimension"));
        EndpointBinding.clear(stack);
        assertTrue(EndpointBinding.read(stack).isEmpty());
        assertEquals("keep", stack.getTag().getString("other_addon"));
    }

    @Test
    void replacingALinkDoesNotLeaveThePreviousEndpoint() {
        ItemStack stack = new ItemStack(Items.STICK);
        EndpointBinding.write(stack, new StorageEndpoint("ae2", Level.OVERWORLD, BlockPos.ZERO));
        StorageEndpoint replacement = new StorageEndpoint("toms_storage", Level.END, new BlockPos(-1, 42, 7));
        EndpointBinding.write(stack, replacement);
        assertEquals(replacement, EndpointBinding.read(stack).orElseThrow());
    }

    @Test
    void unknownBackendsRemainReadableWithoutLoadingTheirMod() {
        ItemStack stack = new ItemStack(Items.STICK);
        StorageEndpoint endpoint = new StorageEndpoint("temporarily_missing", Level.OVERWORLD, BlockPos.ZERO);
        EndpointBinding.write(stack, endpoint);
        assertEquals(endpoint, EndpointBinding.read(stack).orElseThrow());
    }

    @Test
    void incompleteOrMalformedBindingsAreIgnored() {
        ItemStack stack = new ItemStack(Items.STICK);
        CompoundTag root = new CompoundTag();
        root.put("remote_storage_link", new CompoundTag());
        stack.setTag(root);
        assertTrue(EndpointBinding.read(stack).isEmpty());
        CompoundTag link = root.getCompound("remote_storage_link");
        link.putString("backend", "ae2");
        link.putString("dimension", "invalid dimension!");
        link.putLong("pos", 0);
        assertTrue(EndpointBinding.read(stack).isEmpty());
        link.putString("dimension", "minecraft:overworld");
        link.putString("pos", "wrong type");
        assertTrue(EndpointBinding.read(stack).isEmpty());
    }

    @Test
    void endpointsDoNotRetainMutablePositions() {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(1, 2, 3);
        StorageEndpoint endpoint = new StorageEndpoint("ae2", Level.OVERWORLD, mutable);
        mutable.set(9, 9, 9);
        assertEquals(new BlockPos(1, 2, 3), endpoint.pos());
    }
}
