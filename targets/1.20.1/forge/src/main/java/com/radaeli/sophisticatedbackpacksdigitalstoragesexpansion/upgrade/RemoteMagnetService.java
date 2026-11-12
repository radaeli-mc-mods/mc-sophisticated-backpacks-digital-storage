package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.ItemInsertion;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.SourceLocation;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageAdapterRegistry;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageConnection;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageOperation;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageTarget;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageTransferGuard;

/** Tick-driven magnet behavior. Candidate discovery is bounded by the native advanced Magnet range. */
final class RemoteMagnetService {
    private RemoteMagnetService() {
    }

    static boolean tick(RemoteMagnetUpgradeWrapper wrapper, Entity sourceEntity, Level level, BlockPos origin) {
        if (level.isClientSide || !wrapper.isEnabled() || !isInstalled(wrapper)) {
            return false;
        }
        ServerPlayer player = interactingPlayer(sourceEntity, level, origin);
        if (player == null) return false;

        ITrackedContentsItemHandler backpack = wrapper.getStorageWrapper().getInventoryForUpgradeProcessing();
        SourceLocation source = sourceEntity == null
                ? new SourceLocation(level.dimension(), Vec3.atCenterOf(origin))
                : SourceLocation.carriedBy(player);
        StorageConnection connection = StorageAdapterRegistry.connect(player, wrapper.getEndpoint(), source,
                StorageOperation.INSERT);
        StorageTarget remote = connection.target().orElse(null);
        Vec3 center = Vec3.atCenterOf(origin);
        AABB search = new AABB(origin).inflate(wrapper.getRange());
        List<ItemEntity> candidates = level.getEntitiesOfClass(ItemEntity.class, search,
                entity -> entity.isAlive() && !entity.getItem().isEmpty())
                .stream()
                .sorted(Comparator.comparingDouble((ItemEntity entity) -> entity.position().distanceToSqr(center))
                        .thenComparingInt(ItemEntity::getId))
                .toList();

        boolean movedAny = false;
        for (ItemEntity entity : candidates) {
            boolean machineContext = sourceEntity == null;
            if (!isStillEligible(entity, machineContext)) {
                continue;
            }
            ItemStack offered = entity.getItem().copy();

            ItemStack simulatedRemainder = route(wrapper, player, backpack, remote, offered.copy(), true);
            if (simulatedRemainder.getCount() >= offered.getCount()) {
                continue;
            }
            if (!isStillEligible(entity, machineContext)
                    || !ItemStack.isSameItemSameTags(entity.getItem(), offered)) {
                continue;
            }

            ItemStack remainder = route(wrapper, player, backpack, remote, entity.getItem().copy(), false);
            if (remainder.getCount() == entity.getItem().getCount()) {
                continue;
            }
            entity.setItem(remainder);
            if (remainder.isEmpty()) {
                entity.discard();
            }
            movedAny = true;
        }

        if (movedAny) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS, 0.2F, 1.0F);
        }
        return movedAny;
    }

    private static ItemStack route(RemoteMagnetUpgradeWrapper wrapper, ServerPlayer player,
            IItemHandlerSimpleInserter backpack, StorageTarget remote, ItemStack offered, boolean simulate) {
        if (offered.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return DestinationItemFilter.route(wrapper.getSourcePriority(), offered,
                stack -> insertRemote(wrapper, player, remote, stack, simulate),
                stack -> insertBackpack(wrapper, backpack, stack, simulate));
    }

    private static ItemStack insertRemote(RemoteMagnetUpgradeWrapper wrapper, ServerPlayer player,
            StorageTarget remote, ItemStack offered, boolean simulate) {
        if (remote == null || offered.isEmpty()) {
            return offered;
        }
        try (StorageTransferGuard ignored = StorageTransferGuard.blockExternalInsertion(wrapper.getStorageWrapper())) {
            if (!DestinationItemFilter.remote(wrapper.getFilterLogic(), remote, player, offered)) return offered;
            if (simulate) {
                int accepted = ItemInsertion.acceptedCount(offered,
                        remote.simulateInsert(offered.copy(), player));
                return ItemInsertion.remainder(offered, accepted);
            }
            return remote.insert(offered.copy(), player);
        }
    }

    private static ItemStack insertBackpack(RemoteMagnetUpgradeWrapper wrapper, IItemHandlerSimpleInserter backpack, ItemStack offered,
            boolean simulate) {
        return DestinationItemFilter.local(wrapper.getFilterLogic(), offered) ? backpack.insertItem(offered.copy(), simulate) : offered;
    }

    private static ServerPlayer interactingPlayer(Entity entity, Level level, BlockPos origin) {
        if (entity instanceof ServerPlayer player) {
            return validPlayer(player) ? player : null;
        }
        if (entity != null || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return null;
        }
        AABB search = new AABB(origin).inflate(3);
        return serverLevel.players().stream().filter(RemoteMagnetService::validPlayer)
                .filter(player -> search.contains(player.position()))
                .filter(player -> WorldHelper.playerMayInteract(player, origin))
                .min(Comparator.comparingDouble(player -> player.position().distanceToSqr(Vec3.atCenterOf(origin))))
                .orElse(null);
    }

    private static boolean validPlayer(ServerPlayer player) {
        return !(player instanceof FakePlayer) && player.isAlive() && !player.isSpectator();
    }

    private static boolean isStillEligible(ItemEntity entity, boolean machineContext) {
        int pickupDelay = ((com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.mixin.ItemEntityPickupDelayAccessor) entity)
                .sophisticatedBackpackIsAddon$getPickupDelay();
        if (!entity.isAlive() || entity.getItem().isEmpty() || pickupDelay == 32_767
                || RemoteMagnetEligibility.isBlocked(entity)) {
            return false;
        }
        var persistentData = entity.getPersistentData();
        return !persistentData.contains("PreventRemoteMovement")
                || (machineContext && persistentData.contains("AllowMachineRemoteMovement"));
    }

    private static boolean isInstalled(RemoteMagnetUpgradeWrapper wrapper) {
        return wrapper.getStorageWrapper().getUpgradeHandler().getSlotWrappers().containsValue(wrapper);
    }
}
