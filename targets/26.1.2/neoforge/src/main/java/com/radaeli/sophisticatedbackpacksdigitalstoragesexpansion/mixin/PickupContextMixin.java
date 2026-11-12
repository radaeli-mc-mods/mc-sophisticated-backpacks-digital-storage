package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.common.CommonEventHandler;
import org.spongepowered.asm.mixin.Mixin;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemotePickupContext;

@Mixin(CommonEventHandler.class)
public abstract class PickupContextMixin {
    @WrapMethod(method = "onItemPickup(Lnet/neoforged/neoforge/event/entity/player/ItemEntityPickupEvent$Pre;)V")
    private void sophisticatedBackpackIsAddon$withPickupContext(ItemEntityPickupEvent.Pre event,
            Operation<Void> original) {
        try (RemotePickupContext.Scope ignored = RemotePickupContext.open(event.getPlayer(), event.getItemEntity())) {
            original.call(event);
        }
    }
}
