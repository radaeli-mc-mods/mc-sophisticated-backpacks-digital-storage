package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.gametest;

import java.util.UUID;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.GuardedFluidResourceHandler;
import net.neoforged.neoforge.transfer.*;
import net.neoforged.neoforge.transfer.item.*;
import net.neoforged.neoforge.transfer.fluid.*;

/** Real block capabilities reached through native Tunnels interfaces, never a replacement network. */
public final class IntegratedResourceFixture {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("digitalstorage_tests");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "digitalstorage_tests");
    public static final DeferredHolder<net.minecraft.world.level.block.Block, ResourceBlock> BLOCK = BLOCKS.registerBlock("resources", ResourceBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Store>> TYPE = ENTITIES.register("resources", () -> new BlockEntityType<>(Store::new, BLOCK.get()));
    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        bus.addListener(IntegratedResourceFixture::capabilities);
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, TYPE.get(), (store, side) -> store.externalItems);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, TYPE.get(), (store, side) -> store.externalFluids);
    }
    public static final class ResourceBlock extends BaseEntityBlock {
        static final MapCodec<ResourceBlock> CODEC = simpleCodec(ResourceBlock::new);
        ResourceBlock(BlockBehaviour.Properties properties) { super(properties.strength(1)); }
        protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Store(pos, state); }
    }
    public static final class Items extends ItemStacksResourceHandler {
        Items() { super(4); }
        public int getSlots() { return size(); }
        public net.minecraft.world.item.ItemStack getStackInSlot(int slot) { return getResource(slot).toStack(getAmountAsInt(slot)); }
        public void setStackInSlot(int slot, net.minecraft.world.item.ItemStack stack) { set(slot, ItemResource.of(stack), stack.getCount()); }
    }
    public static final class Fluids extends FluidStacksResourceHandler {
        Fluids() { super(1, 2000); }
        public int getFluidAmount() { return getAmountAsInt(0); }
        public net.neoforged.neoforge.fluids.FluidStack getFluid() { return getResource(0).toStack(getAmountAsInt(0)); }
        public void setFluid(net.neoforged.neoforge.fluids.FluidStack stack) { set(0, FluidResource.of(stack), stack.getAmount()); }
    }
    public static final class Store extends BlockEntity {
        public final Items items = new Items();
        public ResourceHandler<ItemResource> externalItems = items;
        public final Fluids fluids = new Fluids();
        public final UUID contentsId = UUID.randomUUID();
        public final GuardedFluidResourceHandler externalFluids = new GuardedFluidResourceHandler(fluids, () -> contentsId);
        Store(BlockPos pos, BlockState state) { super(TYPE.get(), pos, state); }
        @Override protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
            super.saveAdditional(output);
            for (int i=0;i<4;i++) {
                output.store("item"+i, net.minecraft.world.item.ItemStack.OPTIONAL_CODEC, items.getStackInSlot(i).copyWithCount(1));
                output.putInt("amount"+i, items.getAmountAsInt(i));
            }
            output.store("fluid", net.neoforged.neoforge.fluids.FluidStack.OPTIONAL_CODEC, fluids.getFluid());
        }
        @Override protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
            super.loadAdditional(input);
            for (int i=0;i<4;i++) items.setStackInSlot(i, input.read("item"+i, net.minecraft.world.item.ItemStack.OPTIONAL_CODEC)
                    .orElse(net.minecraft.world.item.ItemStack.EMPTY).copyWithCount(input.getIntOr("amount"+i, 0)));
            fluids.setFluid(input.read("fluid", net.neoforged.neoforge.fluids.FluidStack.OPTIONAL_CODEC).orElse(net.neoforged.neoforge.fluids.FluidStack.EMPTY));
        }
    }
}
