package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.mixin;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client.LinkedModelProperty;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client.LinkedUpgradeAppearance;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Invalidates the GUI's rendered-item cache while our externally updated canvas animates. */
@Mixin(RangeSelectItemModel.class)
public abstract class LinkedUpgradeGuiAnimationMixin {
    @Shadow @Final private RangeSelectItemModelProperty property;

    @Inject(method = "update", at = @At("TAIL"))
    private void markLinkedCanvasAnimated(ItemStackRenderState output, ItemStack item,
            ItemModelResolver resolver, ItemDisplayContext displayContext, ClientLevel level,
            ItemOwner owner, int seed, CallbackInfo callback) {
        if (property instanceof LinkedModelProperty && LinkedUpgradeAppearance.isAnimating(item)) {
            output.setAnimated();
        }
    }
}
