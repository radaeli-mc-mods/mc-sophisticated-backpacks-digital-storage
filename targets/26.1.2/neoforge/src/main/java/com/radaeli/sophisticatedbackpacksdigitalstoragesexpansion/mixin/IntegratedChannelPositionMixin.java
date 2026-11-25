package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.IntegratedPositionScope;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** P03/P10: compensate the pinned upstream adapter's missing finally without clearing foreign locks. */
@Pseudo
@Mixin(targets = "org.cyclops.integrateddynamics.core.network.IngredientChannelAdapter", remap = false, priority = 1100)
public abstract class IntegratedChannelPositionMixin {
    @WrapOperation(method = {
            "getMaxQuantity()J",
            "insert(Ljava/lang/Object;Ljava/lang/Object;Lnet/neoforged/neoforge/transfer/transaction/TransactionContext;)Ljava/lang/Object;",
            "extract(JLnet/neoforged/neoforge/transfer/transaction/TransactionContext;)Ljava/lang/Object;",
            "extract(Ljava/lang/Object;Ljava/lang/Object;Lnet/neoforged/neoforge/transfer/transaction/TransactionContext;)Ljava/lang/Object;",
            "finalizeExtraction(Ljava/lang/Object;Ljava/lang/Object;Lorg/apache/commons/lang3/tuple/Pair;Lnet/neoforged/neoforge/transfer/transaction/TransactionContext;)Ljava/lang/Object;"
    }, at = @At(value = "INVOKE",
            target = "Lorg/cyclops/integrateddynamics/api/network/IPositionedAddonsNetworkIngredients;disablePosition(Lorg/cyclops/integrateddynamics/api/part/PartPos;)V"), remap = false)
    private void digitalStorage$acquire(IPositionedAddonsNetworkIngredients<?, ?> network, PartPos pos, Operation<Void> original) {
        boolean owned = !network.isPositionDisabled(pos);
        original.call(network, pos);
        if (owned) IntegratedPositionScope.acquired(network, pos);
    }
    @WrapOperation(method = {
            "getMaxQuantity()J",
            "insert(Ljava/lang/Object;Ljava/lang/Object;Lnet/neoforged/neoforge/transfer/transaction/TransactionContext;)Ljava/lang/Object;",
            "extract(JLnet/neoforged/neoforge/transfer/transaction/TransactionContext;)Ljava/lang/Object;",
            "extract(Ljava/lang/Object;Ljava/lang/Object;Lnet/neoforged/neoforge/transfer/transaction/TransactionContext;)Ljava/lang/Object;",
            "finalizeExtraction(Ljava/lang/Object;Ljava/lang/Object;Lorg/apache/commons/lang3/tuple/Pair;Lnet/neoforged/neoforge/transfer/transaction/TransactionContext;)Ljava/lang/Object;"
    }, at = @At(value = "INVOKE",
            target = "Lorg/cyclops/integrateddynamics/api/network/IPositionedAddonsNetworkIngredients;enablePosition(Lorg/cyclops/integrateddynamics/api/part/PartPos;)V"), remap = false)
    private void digitalStorage$release(IPositionedAddonsNetworkIngredients<?, ?> network, PartPos pos, Operation<Void> original) {
        original.call(network, pos);
        IntegratedPositionScope.released(network, pos);
    }
}
