package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.FluidFilterLogic;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.PumpFluidHandlerFacade;

/** Pump settings reuse Sophisticated's filter, mode components, stack multiplier, and tick lifecycle. */
public final class RemotePumpUpgradeWrapper extends PriorityLinkedStorageUpgradeWrapper<RemotePumpUpgradeWrapper,
        RemotePumpUpgradeItem> implements ITickableUpgrade {
    private final FluidFilterLogic fluidFilterLogic;
    private long lastHandActionTime = Long.MIN_VALUE;

    public RemotePumpUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade,
            Consumer<ItemStack> saveHandler) {
        super(storageWrapper, upgrade, saveHandler);
        fluidFilterLogic = new FluidFilterLogic(Config.SERVER.pumpUpgrade.filterSlots.get(), upgrade, saveHandler);
    }

    public FluidFilterLogic getFluidFilterLogic() {
        return fluidFilterLogic;
    }

    public boolean isInput() {
        return getBoolean("input", true);
    }

    public void setIsInput(boolean value) {
        setBoolean("input", value);
    }

    public boolean shouldInteractWithHand() {
        return getBoolean("interactWithHand", true);
    }

    public void setInteractWithHand(boolean value) {
        setBoolean("interactWithHand", value);
    }

    public boolean shouldInteractWithWorld() {
        return getBoolean("interactWithWorld", true);
    }

    public void setInteractWithWorld(boolean value) {
        setBoolean("interactWithWorld", value);
    }

    public boolean shouldInteractWithFluidHandlers() {
        return getBoolean("interactWithFluidHandlers", true);
    }

    public void setInteractWithFluidHandlers(boolean value) {
        setBoolean("interactWithFluidHandlers", value);
    }

    public int getMaxInputOutput() {
        double stackRatio = Config.SERVER.pumpUpgrade.stackMultiplierRatio.get();
        int adjustedStackMultiplier = Math.max(1,
                1 + (int) (stackRatio * (storageWrapper.getInventoryHandler().getStackSizeMultiplier() - 1)));
        long configuredLimit = (long) Config.SERVER.pumpUpgrade.maxInputOutput.get()
                * storageWrapper.getNumberOfSlotRows() * adjustedStackMultiplier;
        // Native Pump has a one-bucket minimum, not a maximum. Keep that minimum
        // before applying our one-reservation limit, so small backpacks can pump world sources.
        return (int) Math.min(PumpFluidHandlerFacade.MAX_OPERATION_MB, Math.max(1_000, configuredLimit));
    }

    public int getFluidFilterCount() {
        return fluidFilterLogic.getNumberOfFluidFilters();
    }

    public boolean hasPendingReservation() {
        return PumpReservation.hasPending(upgrade);
    }

    public FluidStack getPendingFluid(Level level) {
        return PumpReservation.fluid(upgrade, level.registryAccess());
    }

    public void saveReservationChange() {
        save();
    }

    public void reserveFluid(FluidStack fluid, boolean remoteOrigin, Level level) {
        PumpReservation.reserve(upgrade, fluid, remoteOrigin, level.registryAccess());
        save();
    }

    public void updatePendingFluid(FluidStack fluid, Level level) {
        PumpReservation.updateFluid(upgrade, fluid, level.registryAccess());
        save();
    }

    public boolean pendingFluidHasRemoteOrigin() {
        return PumpReservation.hasRemoteOrigin(upgrade);
    }

    public void setPendingState(PumpReservation.State state) {
        PumpReservation.setState(upgrade, state);
        save();
    }

    public void clearPendingFluid() {
        PumpReservation.clear(upgrade);
        save();
    }

    public void setOperationCooldown(Level level, int ticks, boolean handInteraction) {
        setCooldown(level, ticks);
        if (handInteraction) {
            lastHandActionTime = level.getGameTime();
        }
    }

    public int getNoActionCooldown(Level level) {
        return lastHandActionTime != Long.MIN_VALUE && lastHandActionTime + 30 > level.getGameTime() ? 3 : 40;
    }

    @Override
    public void tick(Entity entity, Level level, BlockPos pos) {
        if (level.isClientSide || isInCooldown(level) || (!isEnabled() && !hasPendingReservation())) {
            return;
        }
        PumpService.tick(this, entity, level, pos);
    }

    private boolean getBoolean(String key, boolean fallback) {
        return !upgrade.hasTag() || !upgrade.getTag().contains(key) ? fallback : upgrade.getTag().getBoolean(key);
    }

    private void setBoolean(String key, boolean value) {
        upgrade.getOrCreateTag().putBoolean(key, value);
        save();
    }
}
