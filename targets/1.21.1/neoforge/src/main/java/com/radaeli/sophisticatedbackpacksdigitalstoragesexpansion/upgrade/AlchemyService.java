package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyCondition;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyFilterAttribute;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeWrapper.AlchemyItemDefinition;

/** One at-a-time native Alchemy application with a durable item reservation and transient wearer binding. */
final class AlchemyService {
    private static final int CHECK_COOLDOWN_TICKS = 5;
    private static final Map<IStorageWrapper, ActiveUse> ACTIVE = new WeakHashMap<>();

    private AlchemyService() {
    }

    static void tick(RemoteAlchemyUpgradeWrapper wrapper, ServerPlayer player) {
        Level level = player.level();
        IStorageWrapper storage = wrapper.getStorageWrapper();
        if (!isInstalled(wrapper)) {
            ACTIVE.remove(storage);
            return;
        }
        if (wrapper.hasPendingReservation()) {
            AlchemyReservation.State state = AlchemyReservation.state(wrapper.getUpgradeStack()).orElse(AlchemyReservation.State.RECOVERING);
            ActiveUse active = ACTIVE.get(storage);
            if (state == AlchemyReservation.State.USING && active != null && active.wearer() == player
                    && wrapper.isEnabled() && player.isAlive()) {
                advance(wrapper, player, active);
                return;
            }
            ACTIVE.remove(storage);
            recoverPending(wrapper, player);
            wrapper.setServiceCooldown(level, CHECK_COOLDOWN_TICKS);
            return;
        }
        if (!wrapper.isEnabled() || wrapper.isInCooldown(level)) {
            return;
        }

        List<AlchemyFilterAttribute> rules = wrapper.getAlchemyAttributes();
        for (int slot = 0; slot < rules.size(); slot++) {
            AlchemyFilterAttribute rule = rules.get(slot);
            ItemStack filter = wrapper.getFilterLogic().getFilterHandler().getStackInSlot(slot);
            if (filter.isEmpty() || rule.condition() == AlchemyCondition.NEVER
                    || !rule.condition().test(player, rule.value())) {
                continue;
            }
            List<UpgradeItemSources.SourceItem> candidates = UpgradeItemSources.find(wrapper, player,
                    wrapper.getSourcePriority(), stack -> wrapper.getFilterLogic().stackMatchesFilter(stack, filter), java.util.Optional.of(filter.getItem()), 1);
            for (UpgradeItemSources.SourceItem candidate : candidates) {
                AlchemyItemDefinition definition = findDefinition(wrapper, player, filter, candidate.stack());
                if (definition == null) {
                    continue;
                }
                ItemStack extracted = UpgradeItemSources.extract(wrapper, player, candidate, 1);
                if (extracted.isEmpty()) {
                    continue;
                }
                if (!rule.condition().test(player, rule.value())
                        || (definition = findDefinition(wrapper, player, filter, extracted)) == null) {
                    ItemStack remainder = UpgradeItemSources.returnToSource(wrapper, player, candidate, extracted);
                    if (!remainder.isEmpty()) UpgradeItemSources.recover(wrapper, player, remainder);
                    continue;
                }
                ItemStack reserved = extracted.copyWithCount(1);
                if (!AlchemyReservation.reserve(wrapper.getUpgradeStack(), reserved, candidate.remote(), player.registryAccess())) {
                    ItemStack remainder = UpgradeItemSources.returnToSource(wrapper, player, candidate, extracted);
                    if (!remainder.isEmpty()) UpgradeItemSources.recover(wrapper, player, remainder);
                    wrapper.setServiceCooldown(level, CHECK_COOLDOWN_TICKS);
                    return;
                }
                AlchemyReservation.setState(wrapper.getUpgradeStack(), AlchemyReservation.State.USING);
                wrapper.saveReservationChange();

                try {
                    int duration = definition.startUsing().applyAsInt(reserved, player);
                    AlchemyReservation.updateStack(wrapper.getUpgradeStack(), reserved, player.registryAccess());
                    wrapper.saveReservationChange();
                    if (duration <= 0) {
                        finish(wrapper, player, definition, reserved);
                    } else {
                        ACTIVE.put(storage, new ActiveUse(player, definition, reserved, duration, duration,
                                level.getGameTime() + 5, level.getGameTime()));
                    }
                } catch (RuntimeException exception) {
                    ACTIVE.remove(storage);
                    AlchemyReservation.setState(wrapper.getUpgradeStack(), AlchemyReservation.State.RECOVERING);
                    wrapper.saveReservationChange();
                    recoverPending(wrapper, player);
                    throw exception;
                }
                return;
            }
        }
        wrapper.setServiceCooldown(level, CHECK_COOLDOWN_TICKS);
    }

    private static AlchemyItemDefinition findDefinition(RemoteAlchemyUpgradeWrapper wrapper, LivingEntity wearer,
            ItemStack filter, ItemStack candidate) {
        for (AlchemyItemDefinition definition : wrapper.getItemDefinitions()) {
            if (definition.filter().test(candidate)
                    && definition.stackMatches().test(candidate, filter, wrapper.shouldMatchAllEffects(),
                            wrapper.shouldMatchEffectDuration(), wrapper.shouldMatchEffectAmplifier())
                    && definition.canApply().test(wearer, candidate, wrapper.shouldMatchAllEffects(),
                            wrapper.shouldMatchEffectAmplifier())) {
                return definition;
            }
        }
        return null;
    }

    private static void advance(RemoteAlchemyUpgradeWrapper wrapper, ServerPlayer player, ActiveUse active) {
        long gameTime = player.level().getGameTime();
        if (!active.wearer().isAlive() || active.wearer() != player || gameTime - active.lastSeenTime() > 6) {
            ACTIVE.remove(wrapper.getStorageWrapper());
            AlchemyReservation.setState(wrapper.getUpgradeStack(), AlchemyReservation.State.RECOVERING);
            wrapper.saveReservationChange();
            recoverPending(wrapper, player);
            return;
        }
        // The wrapper callback runs every tick. Advance by elapsed game ticks so
        // native use durations remain real-time durations, even if callbacks are
        // delayed by a few ticks.
        int elapsed = (int) Math.max(1, Math.min(Integer.MAX_VALUE, gameTime - active.lastSeenTime()));
        int remaining = active.remainingTicks() - elapsed;
        if (remaining <= 0) {
            ACTIVE.remove(wrapper.getStorageWrapper());
            if (active.definition().hasItemUseEffects()) {
                triggerUseSound(player.level(), player, active.stack());
            }
            finish(wrapper, player, active.definition(), active.stack());
            return;
        }
        if (active.definition().hasItemUseEffects()
                && remaining % 4 == 0
                && active.durationTicks() - remaining > (int) (active.stack().getUseDuration(player) * 0.21875F)) {
            triggerUseSound(player.level(), player, active.stack());
        }
        ACTIVE.put(wrapper.getStorageWrapper(), active.withRemaining(remaining, gameTime + 5, gameTime));
    }

    private static void finish(RemoteAlchemyUpgradeWrapper wrapper, ServerPlayer player,
            AlchemyItemDefinition definition, ItemStack reserved) {
        if (!player.isAlive()) {
            AlchemyReservation.setState(wrapper.getUpgradeStack(), AlchemyReservation.State.RECOVERING);
            wrapper.saveReservationChange();
            recoverPending(wrapper, player);
            return;
        }
        ItemStack returned = definition.finishUsing().apply(reserved.copy(), player);
        AlchemyReservation.updateStack(wrapper.getUpgradeStack(), returned, player.registryAccess());
        AlchemyReservation.setState(wrapper.getUpgradeStack(), AlchemyReservation.State.RECOVERING);
        wrapper.saveReservationChange();
        if (returned.isEmpty()) {
            AlchemyReservation.clear(wrapper.getUpgradeStack());
            wrapper.saveReservationChange();
            return;
        }
        ItemStack remaining = UpgradeItemSources.returnToLinkedNetwork(wrapper, player, returned);
        if (!remaining.isEmpty()) {
            remaining = wrapper.getStorageWrapper().getInventoryForUpgradeProcessing().insertItem(remaining, false);
        }
        if (!remaining.isEmpty()) {
            player.getInventory().placeItemBackInInventory(remaining);
        }
        AlchemyReservation.clear(wrapper.getUpgradeStack());
        wrapper.saveReservationChange();
    }

    private static void triggerUseSound(Level level, LivingEntity wearer, ItemStack stack) {
        if (stack.getUseAnimation() == UseAnim.DRINK) {
            level.playSound(null, wearer.getX(), wearer.getY(), wearer.getZ(), stack.getDrinkingSound(),
                    wearer.getSoundSource(), 0.5F,
                    0.9F + level.getRandom().nextFloat() * 0.1F);
        } else if (stack.getUseAnimation() == UseAnim.EAT) {
            level.playSound(null, wearer.getX(), wearer.getY(), wearer.getZ(), wearer.getEatingSound(stack),
                    SoundSource.PLAYERS, 0.5F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        }
    }

    private static void recoverPending(RemoteAlchemyUpgradeWrapper wrapper, ServerPlayer player) {
        ItemStack pending = AlchemyReservation.getStack(wrapper.getUpgradeStack(), player.registryAccess());
        if (pending.isEmpty()) {
            AlchemyReservation.clear(wrapper.getUpgradeStack());
            wrapper.saveReservationChange();
            return;
        }
        AlchemyReservation.setState(wrapper.getUpgradeStack(), AlchemyReservation.State.RECOVERING);
        wrapper.saveReservationChange();
        ItemStack remaining = AlchemyReservation.isRemote(wrapper.getUpgradeStack())
                ? UpgradeItemSources.returnToLinkedNetwork(wrapper, player, pending)
                : wrapper.getStorageWrapper().getInventoryForUpgradeProcessing().insertItem(pending, false);
        if (!remaining.isEmpty()) {
            UpgradeItemSources.recover(wrapper, player, remaining);
        }
        AlchemyReservation.clear(wrapper.getUpgradeStack());
        wrapper.saveReservationChange();
    }

    private static boolean isInstalled(RemoteAlchemyUpgradeWrapper wrapper) {
        return wrapper.getStorageWrapper().getUpgradeHandler().getSlotWrappers().containsValue(wrapper);
    }

    private record ActiveUse(ServerPlayer wearer, AlchemyItemDefinition definition, ItemStack stack,
            int durationTicks, int remainingTicks, long nextCheckTime, long lastSeenTime) {
        private ActiveUse withRemaining(int remaining, long nextCheck, long lastSeen) {
            return new ActiveUse(wearer, definition, stack, durationTicks, remaining, nextCheck, lastSeen);
        }

        private ActiveUse withLastSeen(long lastSeen) {
            return new ActiveUse(wearer, definition, stack, durationTicks, remainingTicks, nextCheckTime, lastSeen);
        }
    }
}
