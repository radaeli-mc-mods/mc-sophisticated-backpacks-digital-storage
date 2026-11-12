package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.node.GraphNetworkComponent;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.security.PlatformSecurityNetworkComponent;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemPlayerValidator;
import com.refinedmods.refinedstorage.common.controller.ControllerBlockEntity;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/** Fluid adapter for the Refined Storage version pinned by P01. Its API amounts are mB. */
public final class RefinedStorageFluidStorageAdapter implements FluidStorageAdapter {
    public static final String BACKEND_ID = RefinedStorageAdapter.BACKEND_ID;

    @Override
    public String backendId() {
        return BACKEND_ID;
    }

    @Override
    public Optional<FluidStorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint) {
        if (level.getBlockEntity(endpoint.pos()) instanceof ControllerBlockEntity controller) {
            return Optional.of(new Target(endpoint, controller));
        }
        return Optional.empty();
    }

    private record Target(StorageEndpoint endpoint, ControllerBlockEntity controller) implements FluidStorageTarget {
        private Optional<StorageNetworkComponent> storage() {
            return Optional.ofNullable(controller.getNetworkForItem())
                    .map(network -> network.getComponent(StorageNetworkComponent.class));
        }

        @Override
        public boolean isReachable(ServerPlayer player, SourceLocation source, FluidStorageAccess access) {
            Network network = controller.getNetworkForItem();
            StorageNetworkComponent storage = network == null ? null
                    : network.getComponent(StorageNetworkComponent.class);
            if (storage == null) {
                return false;
            }

            PlatformSecurityNetworkComponent security = network.getComponent(PlatformSecurityNetworkComponent.class);
            BuiltinPermission permission = switch (access) {
                case QUERY -> BuiltinPermission.OPEN;
                case INSERT -> BuiltinPermission.INSERT;
                case EXTRACT -> BuiltinPermission.EXTRACT;
            };
            if (security != null && !security.isAllowed(permission, player)) {
                return false;
            }

            GraphNetworkComponent graph = network.getComponent(GraphNetworkComponent.class);
            if (graph == null) {
                return false;
            }
            NetworkItemPlayerValidator.PlayerCoordinates coordinates =
                    new NetworkItemPlayerValidator.PlayerCoordinates(source.dimension(), source.position());
            return graph.getContainers(NetworkItemPlayerValidator.class).stream()
                    .anyMatch(validator -> validator.isValid(coordinates));
        }

        @Override
        public List<FluidStorageAmount> getAvailableFluids(ServerPlayer player) {
            List<FluidStorageAmount> result = new ArrayList<>();
            storage().ifPresent(storage -> storage.getAll().forEach(amount -> {
                ResourceKey resource = amount.resource();
                if (resource instanceof FluidResource fluidResource && amount.amount() > 0) {
                    FluidStack stack = new FluidStack(fluidResource.fluid(), 1);
                    stack.applyComponents(fluidResource.components());
                    result.add(new FluidStorageAmount(FluidStorageKey.of(stack), amount.amount()));
                }
            }));
            return List.copyOf(result);
        }

        @Override
        public long simulateInsert(FluidStack stack, ServerPlayer player) {
            if (stack.isEmpty()) {
                return 0;
            }
            return storage().map(storage -> storage.insert(resource(stack), stack.getAmount(), Action.SIMULATE,
                    actor(player))).orElse(0L);
        }

        @Override
        public FluidStack insert(FluidStack stack, ServerPlayer player) {
            if (stack.isEmpty()) {
                return FluidStack.EMPTY;
            }
            long accepted = storage().map(storage -> storage.insert(resource(stack), stack.getAmount(),
                    Action.EXECUTE, actor(player))).orElse(0L);
            return remainder(stack, accepted);
        }

        @Override
        public long simulateExtract(FluidStorageKey key, long amountMb, ServerPlayer player) {
            if (amountMb <= 0) {
                return 0;
            }
            return storage().map(storage -> storage.extract(resource(key), amountMb,
                    Action.SIMULATE, actor(player))).orElse(0L);
        }

        @Override
        public FluidStack extract(FluidStorageKey key, long amountMb, ServerPlayer player) {
            if (amountMb <= 0) {
                return FluidStack.EMPTY;
            }
            long extracted = storage().map(storage -> storage.extract(resource(key), amountMb,
                    Action.EXECUTE, actor(player))).orElse(0L);
            return key.toStack((int) Math.min(Math.max(0, extracted), Integer.MAX_VALUE));
        }

        private static FluidResource resource(FluidStack stack) {
            return new FluidResource(stack.getFluid(), stack.getComponentsPatch());
        }

        private static FluidResource resource(FluidStorageKey key) {
            return new FluidResource(key.fluid(), key.components());
        }

        private static Actor actor(ServerPlayer player) {
            return () -> player.getGameProfile().getName();
        }

        private static FluidStack remainder(FluidStack offered, long accepted) {
            FluidStack remainder = offered.copy();
            remainder.setAmount(offered.getAmount() - (int) Math.min(offered.getAmount(), Math.max(0, accepted)));
            return remainder.isEmpty() ? FluidStack.EMPTY : remainder;
        }
    }
}
