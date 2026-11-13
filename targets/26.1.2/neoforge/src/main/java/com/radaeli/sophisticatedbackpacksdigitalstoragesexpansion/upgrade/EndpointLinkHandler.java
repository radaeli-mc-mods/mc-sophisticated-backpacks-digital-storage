package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.EndpointBinding;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.SourceLocation;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageAdapterRegistry;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageConnection;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageEndpoint;

/** Shared linking interaction for manually linked upgrades. */
public final class EndpointLinkHandler {
    private EndpointLinkHandler() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()) {
            return;
        }
        ItemStack held = player.getItemInHand(event.getHand());
        if (!(held.getItem() instanceof ManuallyLinkedUpgrade item) || !event.getLevel().mayInteract(player, event.getPos())) {
            return;
        }
        var identified = StorageAdapterRegistry.identify(event.getLevel(), event.getPos(),
                event.getHitVec(), SourceLocation.carriedBy(player));
        if (identified.isEmpty()) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!item.canChangeBinding(held)) {
            player.sendSystemMessage(Component.translatable("message.sophisticated_backpacks_digital_storages_expansion.pending_reservation"), true);
            return;
        }
        StorageEndpoint endpoint = identified.get();
        StorageConnection connection = StorageAdapterRegistry.connectForBinding(player, endpoint, SourceLocation.carriedBy(player));
        if (connection.target().isEmpty()) {
            player.sendSystemMessage(Component.translatable(connection.status().messageKey()), true);
            return;
        }
        var previous = EndpointBinding.read(held).map(StorageEndpoint::backendId);
        EndpointBinding.write(held, endpoint);
        EndpointBinding.markLinked(held, event.getLevel().getGameTime(), previous);
        player.sendSystemMessage(Component.translatable("message.sophisticated_backpacks_digital_storages_expansion.linked"), true);
    }
}
