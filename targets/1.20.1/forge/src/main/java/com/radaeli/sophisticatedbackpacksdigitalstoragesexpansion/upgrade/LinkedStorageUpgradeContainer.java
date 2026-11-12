package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageEndpoint;

public abstract class LinkedStorageUpgradeContainer<W extends LinkedStorageUpgradeWrapper<?, ?>,
        C extends LinkedStorageUpgradeContainer<W, C>> extends UpgradeContainerBase<W, C> {
    private static final String ACTION = "remoteStorageAction";
    private long lastOperationTick = Long.MIN_VALUE;

    protected LinkedStorageUpgradeContainer(Player player, int containerId, W wrapper, UpgradeContainerType<W, C> type) {
        super(player, containerId, wrapper, type);
    }

    @Override
    public final void handleMessage(CompoundTag data) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Optional<UpgradeOperationContext> context = UpgradeOperationContext.fromCurrentMenu(serverPlayer, this);
        if (context.isEmpty()) {
            return;
        }
        // Filters use Sophisticated's own packet and persistence implementation, with the same menu guard.
        handleSettingsPacket(data);
        if (!data.contains(ACTION, Tag.TAG_STRING)) {
            return;
        }
        String action = data.getString(ACTION);
        if (!upgradeWrapper.isEnabled()) {
            serverPlayer.displayClientMessage(Component.translatable("message.sophisticated_backpacks_digital_storages_expansion.disabled"), true);
            return;
        }
        long tick = serverPlayer.serverLevel().getGameTime();
        if (lastOperationTick == tick) {
            return;
        }
        lastOperationTick = tick;
        handleAction(action, context.get());
    }

    protected void handleSettingsPacket(CompoundTag data) {
    }

    protected abstract void handleAction(String action, UpgradeOperationContext context);

    protected final void sendAction(String action) {
        sendDataToServer(() -> {
            CompoundTag data = new CompoundTag();
            data.putString(ACTION, action);
            return data;
        });
    }

    public final Optional<StorageEndpoint> getEndpoint() {
        return upgradeWrapper.getEndpoint();
    }
}
