package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.HungerLevel;

/** Feeding's one-item transaction, using the item's native use and finish hooks. */
final class FeedingService {
    private static final int SUCCESS_COOLDOWN_TICKS = 10;
    private static final int IDLE_COOLDOWN_TICKS = 100;

    private FeedingService() {
    }

    static void tick(RemoteFeedingUpgradeWrapper wrapper, ServerPlayer player) {
        Level level = player.level();
        if (!isInstalled(wrapper)) {
            return;
        }
        if (wrapper.hasPendingReservation()) {
            recoverPending(wrapper, player);
            wrapper.setServiceCooldown(level, IDLE_COOLDOWN_TICKS);
            return;
        }
        if (!wrapper.isEnabled() || wrapper.isInCooldown(level)) {
            return;
        }

        int missingHunger = 20 - player.getFoodData().getFoodLevel();
        if (missingHunger <= 0) {
            wrapper.setServiceCooldown(level, IDLE_COOLDOWN_TICKS);
            return;
        }
        boolean hurt = player.getHealth() < player.getMaxHealth() - 0.1F;
        List<UpgradeItemSources.SourceItem> candidates = UpgradeItemSources.find(wrapper, player,
                wrapper.getSourcePriority(), stack -> isEligible(wrapper, player, stack, missingHunger, hurt), java.util.Optional.empty(), 1);
        for (UpgradeItemSources.SourceItem candidate : candidates) {
            ItemStack extracted = UpgradeItemSources.extract(wrapper, player, candidate, 1);
            if (extracted.isEmpty()) {
                continue;
            }
            ItemStack single = extracted.copyWithCount(1);
            if (!FeedingReservation.reserve(wrapper.getUpgradeStack(), single, candidate.remote(), player.registryAccess())) {
                ItemStack remainder = UpgradeItemSources.returnToSource(wrapper, player, candidate, extracted);
                if (!remainder.isEmpty()) UpgradeItemSources.recover(wrapper, player, remainder);
                wrapper.setServiceCooldown(level, IDLE_COOLDOWN_TICKS);
                return;
            }
            wrapper.saveReservationChange();
            if (!isEligible(wrapper, player, single, 20 - player.getFoodData().getFoodLevel(),
                    player.getHealth() < player.getMaxHealth() - 0.1F)) {
                recoverPending(wrapper, player);
                wrapper.setServiceCooldown(level, IDLE_COOLDOWN_TICKS);
                return;
            }
            boolean consumed = consume(wrapper, player, candidate, single);
            wrapper.setServiceCooldown(level, consumed && player.getFoodData().getFoodLevel() < 20
                    ? SUCCESS_COOLDOWN_TICKS : IDLE_COOLDOWN_TICKS);
            return;
        }
        wrapper.setServiceCooldown(level, IDLE_COOLDOWN_TICKS);
    }

    private static boolean consume(RemoteFeedingUpgradeWrapper wrapper, ServerPlayer player,
            UpgradeItemSources.SourceItem source, ItemStack food) {
        FeedingReservation.setState(wrapper.getUpgradeStack(), FeedingReservation.State.IN_FLIGHT);
        wrapper.saveReservationChange();

        ItemStack originalHand = player.getMainHandItem().copy();
        ItemStack usedStack = food.copy();
        boolean handRestored = false;
        player.setItemInHand(InteractionHand.MAIN_HAND, usedStack);
        try {
            InteractionResult useResult = usedStack.use(player.level(), player, InteractionHand.MAIN_HAND);
            if (!useResult.consumesAction()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, originalHand);
                handRestored = true;
                FeedingReservation.setState(wrapper.getUpgradeStack(), FeedingReservation.State.RECOVERING);
                wrapper.saveReservationChange();
                recoverPending(wrapper, player);
                return false;
            }
            ItemStack finished = usedStack.copy().finishUsingItem(player.level(), player);
            ItemStack returned = EventHooks.onItemUseFinish(player, usedStack,
                    usedStack.getUseDuration(player), finished);
            player.setItemInHand(InteractionHand.MAIN_HAND, originalHand);
            handRestored = true;
            FeedingReservation.updateStack(wrapper.getUpgradeStack(), returned, player.registryAccess());
            FeedingReservation.setState(wrapper.getUpgradeStack(), FeedingReservation.State.RECOVERING);
            wrapper.saveReservationChange();
            if (returned.isEmpty()) {
                FeedingReservation.clear(wrapper.getUpgradeStack());
                wrapper.saveReservationChange();
            } else {
                ConsumptionRecovery.returnPending(wrapper, player, returned, true,
                    FeedingReservation.journal(wrapper.getUpgradeStack(), player.level().registryAccess(), wrapper::saveReservationChange),
                    () -> FeedingReservation.state(wrapper.getUpgradeStack()) == FeedingReservation.State.IN_FLIGHT, () -> { FeedingReservation.clear(wrapper.getUpgradeStack()); wrapper.saveReservationChange(); });
            }
            return true;
        } catch (RuntimeException failure) {
            FeedingReservation.setState(wrapper.getUpgradeStack(), FeedingReservation.State.IN_FLIGHT);
            wrapper.saveReservationChange();
            com.mojang.logging.LogUtils.getLogger().error("Feeding hook failed; {}, reservation quarantined", com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.DeliveryDiagnostics.describe(wrapper.getUpgradeStack(), "consume"), failure);
            return false;
        } finally {
            if (!handRestored) player.setItemInHand(InteractionHand.MAIN_HAND, originalHand);
        }
    }

    private static boolean isEligible(RemoteFeedingUpgradeWrapper wrapper, ServerPlayer player, ItemStack stack,
            int missingHunger, boolean hurt) {
        if (stack.isEmpty() || stack.get(net.minecraft.core.component.DataComponents.FOOD) == null || !wrapper.getFilterLogic().matchesFilter(stack)) {
            return false;
        }
        if (missingHunger <= 0) {
            return false;
        }
        HungerLevel level = wrapper.getFeedAtHungerLevel();
        if (level == HungerLevel.ANY) {
            return true;
        }
        int nutrition = stack.get(net.minecraft.core.component.DataComponents.FOOD).nutrition();
        int threshold = level == HungerLevel.HALF ? nutrition / 2 : nutrition;
        return threshold <= missingHunger || (hurt && wrapper.shouldFeedImmediatelyWhenHurt());
    }

    private static void recoverPending(RemoteFeedingUpgradeWrapper wrapper, ServerPlayer player) {
        if (FeedingReservation.state(wrapper.getUpgradeStack()) == FeedingReservation.State.IN_FLIGHT) return;
        ItemStack pending = FeedingReservation.stack(wrapper.getUpgradeStack(), player.level().registryAccess());
        ConsumptionRecovery.returnPending(wrapper, player, pending, FeedingReservation.remote(wrapper.getUpgradeStack()),
                    FeedingReservation.journal(wrapper.getUpgradeStack(), player.level().registryAccess(), wrapper::saveReservationChange),
                    () -> FeedingReservation.state(wrapper.getUpgradeStack()) == FeedingReservation.State.IN_FLIGHT, () -> { FeedingReservation.clear(wrapper.getUpgradeStack()); wrapper.saveReservationChange(); });
    }

    private static boolean isInstalled(RemoteFeedingUpgradeWrapper wrapper) {
        return wrapper.getStorageWrapper().getUpgradeHandler().getSlotWrappers().containsValue(wrapper);
    }
}
