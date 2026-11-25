package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.gametest;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.GuardedFluidHandler;

public final class IntegratedResourceFixture {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "digitalstorage_tests");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "digitalstorage_tests");
    public static final RegistryObject<ResourceBlock> BLOCK = BLOCKS.register("resources", ResourceBlock::new);
    public static final RegistryObject<BlockEntityType<Store>> TYPE = ENTITIES.register("resources", () -> BlockEntityType.Builder.of(Store::new, BLOCK.get()).build(null));
    static void register(IEventBus bus) { BLOCKS.register(bus); ENTITIES.register(bus); }
    public static final class ResourceBlock extends BaseEntityBlock {
        ResourceBlock() { super(BlockBehaviour.Properties.of().strength(1)); }
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Store(pos, state); }
    }
    public static final class Store extends BlockEntity {
        public final ItemStackHandler items = new ItemStackHandler(4);
        public IItemHandler externalItems = items;
        public final FluidTank fluids = new FluidTank(2000);
        public final UUID contentsId = UUID.randomUUID();
        public final GuardedFluidHandler externalFluids = new GuardedFluidHandler(fluids, () -> contentsId);
        Store(BlockPos pos, BlockState state) { super(TYPE.get(), pos, state); }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            if (cap == ForgeCapabilities.ITEM_HANDLER) return LazyOptional.of(() -> externalItems).cast();
            if (cap == ForgeCapabilities.FLUID_HANDLER) return LazyOptional.of(() -> externalFluids).cast();
            return super.getCapability(cap, side);
        }
    }
}
