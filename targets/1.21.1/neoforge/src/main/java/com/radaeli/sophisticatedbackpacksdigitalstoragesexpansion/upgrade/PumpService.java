package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.wrappers.BucketPickupHandlerWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.FluidStorageAccess;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.FluidStorageAdapterRegistry;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.FluidStorageConnection;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.FluidStorageTarget;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.FluidTransferGuard;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.PumpFluidHandlerFacade;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.PumpStorageRouter;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.PumpTransferService;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.SourceLocation;

/** Server-side, one-reservation-at-a-time fluid transfer coordinator. */
final class PumpService {
    private PumpService() {}

    static void tick(RemotePumpUpgradeWrapper wrapper, Entity entity, Level level, BlockPos pos) {
        if (!isInstalled(wrapper)) return;
        ServerPlayer player = interactingPlayer(entity, level, pos).orElse(null);
        if (player == null) {
            wrapper.setOperationCooldown(level, 40, false);
            return;
        }
        SourceLocation source = entity == null
                ? new SourceLocation(level.dimension(), Vec3.atCenterOf(pos))
                : new SourceLocation(level.dimension(), entity.position());
        UUID contents = wrapper.getStorageWrapper().getContentsUuid().orElse(null);
        // Open before resolving/enumerating the network: it may already include this backpack.
        try (FluidTransferGuard ignored = contents == null ? null : FluidTransferGuard.blockExternalAccess(contents)) {
            if (wrapper.hasPendingReservation()) {
                recover(wrapper, player, level, source);
                wrapper.setOperationCooldown(level, 5, false);
                return;
            }
            if (!wrapper.isEnabled()) {
                wrapper.setOperationCooldown(level, 40, false);
                return;
            }
            FluidStorageConnection connection = FluidStorageAdapterRegistry.connect(player, wrapper.getEndpoint(),
                    source, wrapper.isInput() ? FluidStorageAccess.INSERT : FluidStorageAccess.EXTRACT);
            if (connection.status() == FluidStorageConnection.Status.UNSUPPORTED_FLUIDS) {
                wrapper.setOperationCooldown(level, 40, false);
                return;
            }
            IStorageFluidHandler local = wrapper.getStorageWrapper().getFluidHandler().orElse(null);
            IFluidHandler remote = connection.target().map(target -> new PumpFluidHandlerFacade(target, player,
                    wrapper.isInput() ? PumpFluidHandlerFacade.Direction.HANDLER_TO_NETWORK
                            : PumpFluidHandlerFacade.Direction.NETWORK_TO_HANDLER)).orElse(null);
            PumpStorageRouter storage = new PumpStorageRouter(remote, local,
                    wrapper.getSourcePriority() == SourcePriority.NETWORK_FIRST);
            storage.onExtraction(pending -> persist(wrapper, pending, storage.usedRemoteExtraction(), level));
            if (!storage.isAvailable()) {
                wrapper.setOperationCooldown(level, 40, false);
                return;
            }
            // Preserve native interaction precedence; storage priority is applied inside each interaction.
            if (wrapper.shouldInteractWithHand() && transferHand(wrapper, player, storage, level)) {
                wrapper.setOperationCooldown(level, 3, true);
                return;
            }
            if (wrapper.shouldInteractWithWorld()) {
                int cooldown = transferWorld(wrapper, player, storage, level, pos);
                if (cooldown > 0) {
                    wrapper.setOperationCooldown(level, cooldown, false);
                    return;
                }
            }
            if (wrapper.shouldInteractWithFluidHandlers() && transferAdjacent(wrapper, player, storage, level, pos)) {
                wrapper.setOperationCooldown(level, 20, false);
                return;
            }
        }
        wrapper.setOperationCooldown(level, wrapper.getNoActionCooldown(level), false);
    }

    /** Placed backpacks supply null; use only a live nearby player, never a saved owner or fake player. */
    private static Optional<ServerPlayer> interactingPlayer(Entity entity, Level level, BlockPos pos) {
        if (entity instanceof ServerPlayer player) return validPlayer(player) ? Optional.of(player) : Optional.empty();
        AABB search = new AABB(pos).inflate(3);
        return level.players().stream().filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast)
                .filter(PumpService::validPlayer).filter(player -> search.contains(player.position()))
                .filter(player -> WorldHelper.playerMayInteract(player, pos))
                .min(Comparator.comparingDouble(player -> player.position().distanceToSqr(Vec3.atCenterOf(pos))));
    }

    private static boolean validPlayer(ServerPlayer player) {
        return !(player instanceof FakePlayer) && player.isAlive() && !player.isSpectator();
    }

    private static boolean transferHand(RemotePumpUpgradeWrapper wrapper, ServerPlayer player,
            PumpStorageRouter storage, Level level) {
        for (InteractionHand hand : List.of(InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND)) {
            ItemStack container = player.getItemInHand(hand);
            if (container.isEmpty() || isOwnBackpack(wrapper, container)) continue;
            // Item fluid capabilities require a stack of one; never drain the original stacked container.
            Optional<IFluidHandlerItem> resolved = FluidUtil.getFluidHandler(container.copyWithCount(1));
            if (resolved.isEmpty()) continue;
            IFluidHandlerItem handler = resolved.get();
            if (transfer(wrapper, player, level, handler, storage, wrapper.getMaxInputOutput(), 1)) {
                ItemStack updated = handler.getContainer();
                if (container.getCount() == 1) {
                    player.setItemInHand(hand, updated);
                } else {
                    container.shrink(1);
                    if (!updated.isEmpty() && !player.getInventory().add(updated)) player.drop(updated, false);
                }
                return true;
            }
        }
        return false;
    }

    private static boolean isOwnBackpack(RemotePumpUpgradeWrapper wrapper, ItemStack container) {
        if (container == wrapper.getStorageWrapper().getWrappedStorageStack()) return true;
        Optional<UUID> contents = wrapper.getStorageWrapper().getContentsUuid();
        return contents.filter(id -> id.equals(container.get(ModCoreDataComponents.STORAGE_UUID))).isPresent();
    }

    private static int transferWorld(RemotePumpUpgradeWrapper wrapper, ServerPlayer player,
            PumpStorageRouter storage, Level level, BlockPos origin) {
        return wrapper.isInput() ? pickUpWorldSource(wrapper, player, storage, level, origin)
                : placeIntoWorld(wrapper, player, storage, level, origin) ? 20 : 0;
    }

    private static int pickUpWorldSource(RemotePumpUpgradeWrapper wrapper, ServerPlayer player,
            PumpStorageRouter storage, Level level, BlockPos origin) {
        for (BlockPos target : PumpWorldTargets.pickup(origin)) {
            if (!level.hasChunkAt(target) || !WorldHelper.playerMayInteract(player, target)) continue;
            var fluidState = level.getFluidState(target);
            if (fluidState.isEmpty() || !fluidState.isSource()) continue;
            var block = level.getBlockState(target).getBlock();
            IFluidHandler source = block instanceof BucketPickup pickup
                    ? new BucketPickupHandlerWrapper(player, pickup, level, target)
                    : FluidUtil.getFluidHandler(level, target, null).orElse(null);
            if (source != null && transfer(wrapper, player, level, source, storage, 1_000, 1_000)) {
                return PumpWorldTargets.pickupCooldown(origin, target);
            }
        }
        return 0;
    }

    private static boolean placeIntoWorld(RemotePumpUpgradeWrapper wrapper, ServerPlayer player,
            PumpStorageRouter storage, Level level, BlockPos origin) {
        List<FluidStack> candidates = PumpTransferService.candidates(storage, 1_000,
                candidate -> candidate.getAmount() == 1_000 && matches(wrapper, candidate)
                        && PumpReservation.canEncode(candidate, player.registryAccess()));
        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) continue;
            BlockPos target = origin.relative(direction);
            if (!level.hasChunkAt(target) || !WorldHelper.playerMayInteract(player, target)
                    || !PumpWorldTargets.canPlace(level.getBlockState(target))) continue;
            for (FluidStack candidate : candidates) {
                if (!candidate.getFluidType().canBePlacedInLevel(level, target, candidate)) continue;
                FluidStack extracted = storage.drain(candidate.copy(), IFluidHandler.FluidAction.EXECUTE);
                if (extracted.isEmpty()) continue;
                persist(wrapper, extracted, storage.usedRemoteExtraction(), level);
                if (extracted.getAmount() != 1_000) {
                    wrapper.setPendingState(PumpReservation.State.RECOVERING);
                    return true;
                }
                ReservationFluidHandler reserved = new ReservationFluidHandler(extracted);
                // NeoForge performs placement and dimension-specific vaporization, draining this reservation.
                FluidUtil.tryPlaceFluid(player, level, InteractionHand.MAIN_HAND, target, reserved, extracted.copy());
                persist(wrapper, reserved.contents, storage.usedRemoteExtraction(), level);
                if (wrapper.hasPendingReservation()) wrapper.setPendingState(PumpReservation.State.RECOVERING);
                return true;
            }
        }
        return false;
    }

    /** Isolated source representing an already-persisted one-bucket reservation. */
    private static final class ReservationFluidHandler implements IFluidHandler {
        private FluidStack contents;
        private ReservationFluidHandler(FluidStack contents) { this.contents = contents.copy(); }
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return tank == 0 ? contents.copy() : FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? 1_000 : 0; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return false; }
        @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(contents, resource)) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }
        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            int amount = Math.min(Math.max(0, maxDrain), contents.getAmount());
            if (amount == 0) return FluidStack.EMPTY;
            FluidStack drained = contents.copyWithAmount(amount);
            if (action.execute()) contents.shrink(amount);
            return drained;
        }
    }

    private static boolean transferAdjacent(RemotePumpUpgradeWrapper wrapper, ServerPlayer player,
            PumpStorageRouter storage, Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = pos.relative(direction);
            if (!level.hasChunkAt(adjacent) || !WorldHelper.playerMayInteract(player, adjacent)) continue;
            Optional<IFluidHandler> resolved = FluidUtil.getFluidHandler(level, adjacent, direction.getOpposite());
            if (resolved.isPresent() && transfer(wrapper, player, level, resolved.get(), storage,
                    wrapper.getMaxInputOutput(), 1)) return true;
        }
        return false;
    }

    private static boolean transfer(RemotePumpUpgradeWrapper wrapper, ServerPlayer player, Level level,
            IFluidHandler external, PumpStorageRouter storage, int maximum, int minimum) {
        IFluidHandler source = wrapper.isInput() ? external : storage;
        IFluidHandler destination = wrapper.isInput() ? storage : external;
        return PumpTransferService.transfer(source, destination, maximum, minimum,
                candidate -> matches(wrapper, candidate) && PumpReservation.canEncode(candidate, player.registryAccess()),
                pending -> persist(wrapper, pending,
                        wrapper.isInput() ? storage.hasRemote() : storage.usedRemoteExtraction(), level), wrapper.isInput());
    }

    private static void persist(RemotePumpUpgradeWrapper wrapper, FluidStack pending, boolean remoteOrigin, Level level) {
        if (pending.isEmpty()) {
            wrapper.clearPendingFluid();
        } else {
            boolean recovering = wrapper.hasPendingReservation();
            wrapper.reserveFluid(pending, remoteOrigin, level);
            if (recovering) wrapper.setPendingState(PumpReservation.State.RECOVERING);
        }
    }

    private static boolean matches(RemotePumpUpgradeWrapper wrapper, FluidStack stack) {
        return wrapper.getFluidFilterLogic().fluidMatches(stack);
    }

    private static void recover(RemotePumpUpgradeWrapper wrapper, ServerPlayer player, Level level,
            SourceLocation source) {
        FluidStack pending = wrapper.getPendingFluid(level);
        if (pending.isEmpty()) {
            wrapper.setPendingState(PumpReservation.State.RECOVERING);
            return;
        }
        FluidStorageConnection connection = FluidStorageAdapterRegistry.connect(player, wrapper.getEndpoint(),
                source, FluidStorageAccess.INSERT);
        if (connection.status() == FluidStorageConnection.Status.UNSUPPORTED_FLUIDS) return;
        if (wrapper.pendingFluidHasRemoteOrigin() && connection.target().isPresent()) {
            FluidStorageTarget target = connection.target().get();
            int attempted = (int) Math.clamp(target.simulateInsert(pending.copy(), player), 0, pending.getAmount());
            if (attempted > 0) {
                FluidStack offered = pending.copyWithAmount(attempted);
                FluidStack remainder = target.insert(offered, player);
                int accepted = offered.getAmount() - (remainder.isEmpty() ? 0 : remainder.getAmount());
                pending.shrink(Math.clamp(accepted, 0, attempted));
                // Persist after each destination, before trying the next one.
                persist(wrapper, pending, true, level);
            }
        }
        if (!pending.isEmpty()) {
            IStorageFluidHandler local = wrapper.getStorageWrapper().getFluidHandler().orElse(null);
            if (local != null) {
                int accepted = Math.clamp(local.fill(pending.copy(), IFluidHandler.FluidAction.EXECUTE),
                        0, pending.getAmount());
                pending.shrink(accepted);
            }
        }
        persist(wrapper, pending, wrapper.pendingFluidHasRemoteOrigin(), level);
        if (wrapper.hasPendingReservation()) wrapper.setPendingState(PumpReservation.State.RECOVERING);
    }

    private static boolean isInstalled(RemotePumpUpgradeWrapper wrapper) {
        return wrapper.getStorageWrapper().getUpgradeHandler().getSlotWrappers().containsValue(wrapper);
    }
}
