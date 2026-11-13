package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.mixin.client;

import java.util.List;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.client.render.BackpackBlockModel;
import net.p3pp3rf1y.sophisticatedbackpacks.client.render.BackpackItemModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client.BackpackVariantQuads;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.variant.BackpackVariant;

/**
 * Applies the network skin to the quads Sophisticated feeds to item rendering (inventory, hand, ground and worn:
 * the worn layer also goes through {@code ItemModelResolver}). The variant comes from the stack's color components.
 */
@Mixin(value = BackpackItemModel.class, remap = false)
public abstract class BackpackItemModelMixin {
    @WrapOperation(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/p3pp3rf1y/sophisticatedbackpacks/client/render/BackpackBlockModel$BlockStateModel;getQuads(Lnet/minecraft/world/item/ItemDisplayContext;)Ljava/util/List;"))
    private List<BakedQuad> sophisticatedBackpackIsAddon$skinQuads(BackpackBlockModel.BlockStateModel model,
            ItemDisplayContext context, Operation<List<BakedQuad>> original, @Local(argsOnly = true) ItemStack stack) {
        List<BakedQuad> quads = original.call(model, context);
        return BackpackVariantQuads.apply(quads, BackpackVariant.fromStack(stack).orElse(null));
    }
}
