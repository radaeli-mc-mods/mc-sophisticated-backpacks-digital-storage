package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.FluidFilterContainer;

/** GUI settings and native Sophisticated fluid filter packet handling. */
public final class RemotePumpUpgradeContainer
        extends PriorityLinkedStorageUpgradeContainer<RemotePumpUpgradeWrapper, RemotePumpUpgradeContainer> {
    private static final String DATA_IS_INPUT = "pumpIsInput";
    private static final String DATA_HAND = "pumpInteractWithHand";
    private static final String DATA_WORLD = "pumpInteractWithWorld";
    private static final String DATA_HANDLERS = "pumpInteractWithFluidHandlers";
    private final FluidFilterContainer fluidFilterContainer;

    public RemotePumpUpgradeContainer(Player player, int containerId, RemotePumpUpgradeWrapper wrapper,
            UpgradeContainerType<RemotePumpUpgradeWrapper, RemotePumpUpgradeContainer> type) {
        super(player, containerId, wrapper, type);
        fluidFilterContainer = new FluidFilterContainer(player, this, wrapper::getFluidFilterLogic);
    }

    public FluidFilterContainer getFluidFilterContainer() {
        return fluidFilterContainer;
    }

    public boolean isInput() { return upgradeWrapper.isInput(); }
    public boolean shouldInteractWithHand() { return upgradeWrapper.shouldInteractWithHand(); }
    public boolean shouldInteractWithWorld() { return upgradeWrapper.shouldInteractWithWorld(); }
    public boolean shouldInteractWithFluidHandlers() { return upgradeWrapper.shouldInteractWithFluidHandlers(); }
    public int getFluidFilterCount() { return upgradeWrapper.getFluidFilterCount(); }
    public boolean hasPendingReservation() { return upgradeWrapper.hasPendingReservation(); }

    public Component getPumpStatus() {
        if (hasPendingReservation()) {
            return Component.translatable("upgrade.sophisticated_backpacks_digital_storages_expansion.pump.pending");
        }
        return getEndpoint().filter(endpoint -> "toms_storage".equals(endpoint.backendId()))
                .map(ignored -> Component.translatable("message.sophisticated_backpacks_digital_storages_expansion.unsupported_fluids"))
                .orElseGet(() -> Component.empty());
    }

    public void toggleInput() { sendBoolean(DATA_IS_INPUT, !isInput()); }
    public void toggleHand() { sendBoolean(DATA_HAND, !shouldInteractWithHand()); }
    public void toggleWorld() { sendBoolean(DATA_WORLD, !shouldInteractWithWorld()); }
    public void toggleHandlers() { sendBoolean(DATA_HANDLERS, !shouldInteractWithFluidHandlers()); }

    private void sendBoolean(String key, boolean value) {
        sendDataToServer(() -> {
            CompoundTag data = new CompoundTag();
            data.putBoolean(key, value);
            return data;
        });
    }

    @Override
    protected void handleSettingsPacket(CompoundTag data) {
        super.handleSettingsPacket(data);
        fluidFilterContainer.handlePacket(data);
        if (data.contains(DATA_IS_INPUT)) upgradeWrapper.setIsInput(data.getBoolean(DATA_IS_INPUT));
        if (data.contains(DATA_HAND)) upgradeWrapper.setInteractWithHand(data.getBoolean(DATA_HAND));
        if (data.contains(DATA_WORLD)) upgradeWrapper.setInteractWithWorld(data.getBoolean(DATA_WORLD));
        if (data.contains(DATA_HANDLERS)) upgradeWrapper.setInteractWithFluidHandlers(data.getBoolean(DATA_HANDLERS));
    }

    @Override
    protected void handleAction(String action, UpgradeOperationContext context) {
        // Pump operates from its native tick callback; GUI has settings only.
    }
}
