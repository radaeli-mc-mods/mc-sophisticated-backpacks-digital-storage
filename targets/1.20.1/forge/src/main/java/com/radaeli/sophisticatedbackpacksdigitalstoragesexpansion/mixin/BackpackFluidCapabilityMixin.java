package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.mixin;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.GuardedFluidHandler;

/** Guards capability operations even when another mod has cached the returned handler. */
@Mixin(value = BackpackBlockEntity.class, remap = false)
public abstract class BackpackFluidCapabilityMixin {
    @Shadow(remap = false) public abstract net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper getStorageWrapper();

    @Inject(method = "getCapability", at = @At("RETURN"), cancellable = true, remap = false)
    @SuppressWarnings("unchecked")
    private <T> void digitalStoragesExpansion$guardFluidCapability(Capability<T> capability, Direction side,
            CallbackInfoReturnable<LazyOptional<T>> cir) {
        if (capability == ForgeCapabilities.FLUID_HANDLER && cir.getReturnValue().isPresent()) {
            LazyOptional<IFluidHandler> mapped = ((LazyOptional<IFluidHandler>) (LazyOptional<?>) cir.getReturnValue())
                    .lazyMap(handler -> handler instanceof GuardedFluidHandler ? handler
                            : new GuardedFluidHandler(handler,
                                    () -> getStorageWrapper().getContentsUuid().orElse(null)));
            cir.setReturnValue((LazyOptional<T>) (LazyOptional<?>) mapped);
        }
    }
}
