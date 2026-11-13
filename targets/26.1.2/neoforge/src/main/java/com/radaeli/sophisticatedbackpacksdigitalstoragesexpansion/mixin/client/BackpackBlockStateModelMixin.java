package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.mixin.client;

import java.util.List;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.client.render.BackpackBlockModel;
import org.spongepowered.asm.mixin.Mixin;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client.BackpackVariantQuads;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.variant.BackpackVariant;

/** Applies the network skin to placed backpacks, reading the variant from the block entity's color components. */
@Mixin(value = BackpackBlockModel.BlockStateModel.class, remap = false)
public abstract class BackpackBlockStateModelMixin {
    @WrapMethod(method = "collectParts(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;Ljava/util/List;)V")
    private void sophisticatedBackpackIsAddon$skinParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
            RandomSource random, List<BlockStateModelPart> parts, Operation<Void> original) {
        int before = parts.size();
        original.call(level, pos, state, random, parts);
        if (!(level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack)) return;
        var wrapper = backpack.getBackpackWrapper();
        BackpackVariant variant = BackpackVariant.fromColors(wrapper.getMainColor(), wrapper.getAccentColor()).orElse(null);
        if (variant == null) return;
        for (int i = before; i < parts.size(); i++) {
            parts.set(i, BackpackVariantQuads.apply(parts.get(i), variant));
        }
    }
}
