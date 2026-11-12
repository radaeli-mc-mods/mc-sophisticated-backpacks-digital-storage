package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.common.CommonEventHandler;
import org.spongepowered.asm.mixin.Mixin;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemotePickupContext;

@Mixin(value = CommonEventHandler.class, remap = false)
public abstract class PickupContextMixin {
    @WrapMethod(method = "onItemPickup(Lnet/minecraftforge/event/entity/player/EntityItemPickupEvent;)V", remap = false)
    private void sophisticatedBackpackIsAddon$withPickupContext(EntityItemPickupEvent event,
            Operation<Void> original) {
        try (RemotePickupContext.Scope ignored = RemotePickupContext.open(event.getEntity(), event.getItem())) {
            original.call(event);
        }
    }
}
