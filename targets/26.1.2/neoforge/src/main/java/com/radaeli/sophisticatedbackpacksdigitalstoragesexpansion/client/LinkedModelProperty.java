package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;

public record LinkedModelProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<LinkedModelProperty> CODEC = MapCodec.unit(new LinkedModelProperty());
    @Override public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
        return LinkedUpgradeAppearance.modelState(stack);
    }
    @Override public MapCodec<LinkedModelProperty> type() { return CODEC; }
}
