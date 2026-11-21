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
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.GuardedFluidHandler;

/** Real block capabilities reached through native Tunnels interfaces, never a replacement network. */
public final class IntegratedResourceFixture {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("digitalstorage_tests");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "digitalstorage_tests");
    public static final DeferredHolder<net.minecraft.world.level.block.Block, ResourceBlock> BLOCK = BLOCKS.register("resources", ResourceBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Store>> TYPE = ENTITIES.register("resources", () -> BlockEntityType.Builder.of(Store::new, BLOCK.get()).build(null));
    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        bus.addListener(IntegratedResourceFixture::capabilities);
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TYPE.get(), (store, side) -> store.externalItems);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TYPE.get(), (store, side) -> store.externalFluids);
    }
    public static final class ResourceBlock extends BaseEntityBlock {
        static final MapCodec<ResourceBlock> CODEC = simpleCodec(properties -> new ResourceBlock());
        ResourceBlock() { super(BlockBehaviour.Properties.of().strength(1)); }
        protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Store(pos, state); }
    }
    public static final class Store extends BlockEntity {
        public final ItemStackHandler items = new ItemStackHandler(4);
        public IItemHandler externalItems = items;
        public final FluidTank fluids = new FluidTank(2000);
        public final UUID contentsId = UUID.randomUUID();
        public final GuardedFluidHandler externalFluids = new GuardedFluidHandler(fluids, () -> contentsId);
        Store(BlockPos pos, BlockState state) { super(TYPE.get(), pos, state); }
    }
}
