package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.mixin.client;

import java.util.List;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client.BackpackVariantQuads;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.variant.BackpackVariant;

/** Applies the network skin to Sophisticated's baked backpack model without replacing (or wrapping) it. */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.client.render.BackpackDynamicModel$BackpackBakedModel", remap = false)
public abstract class BackpackBakedModelMixin {
    @ModifyReturnValue(method = "getQuads", at = @At("RETURN"))
    private List<BakedQuad> sophisticatedBackpackIsAddon$skinQuads(List<BakedQuad> quads, BlockState state,
            Direction side, RandomSource random, ModelData data, RenderType renderType) {
        BackpackVariant variant = state == null ? BackpackVariantQuads.itemVariant() : data.get(BackpackVariantQuads.VARIANT);
        return BackpackVariantQuads.apply(quads, variant);
    }

    @ModifyReturnValue(method = "getModelData", at = @At("RETURN"))
    private ModelData sophisticatedBackpackIsAddon$addVariant(ModelData original, BlockAndTintGetter level,
            BlockPos pos, BlockState state, ModelData data) {
        if (!(level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack)) return original;
        var wrapper = backpack.getBackpackWrapper();
        return BackpackVariant.fromColors(wrapper.getMainColor(), wrapper.getAccentColor())
                .map(variant -> original.derive().with(BackpackVariantQuads.VARIANT, variant).build())
                .orElse(original);
    }
}
