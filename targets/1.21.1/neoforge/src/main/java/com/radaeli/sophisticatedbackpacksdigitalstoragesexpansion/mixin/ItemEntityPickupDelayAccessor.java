package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the native infinite pickup delay for Magnet parity with Sophisticated. */
@Mixin(ItemEntity.class)
public interface ItemEntityPickupDelayAccessor {
    @Accessor("pickupDelay")
    int sophisticatedBackpackIsAddon$getPickupDelay();
}
