package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.mixin;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.GuardedFluidResourceHandler;

/** Guards capability operations even when another mod has cached the returned handler. */
@Mixin(BackpackBlockEntity.class)
public abstract class BackpackFluidCapabilityMixin {
    @Shadow public abstract net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper getStorageWrapper();

    @Inject(method = "getExternalFluidHandler", at = @At("RETURN"), cancellable = true)
    private void digitalStoragesExpansion$guardFluidCapability(Direction side,
            CallbackInfoReturnable<ResourceHandler<FluidResource>> cir) {
        ResourceHandler<FluidResource> handler = cir.getReturnValue();
        if (handler != null && !(handler instanceof GuardedFluidResourceHandler)) {
            cir.setReturnValue(new GuardedFluidResourceHandler(handler,
                    () -> getStorageWrapper().getContentsUuid().orElse(null)));
        }
    }
}
