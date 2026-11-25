package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.gametest.mixin;

import java.util.function.Supplier;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Test-only: Tom's 1.7.1 client supplier fails Forge's dev-only referent validation.
 * Production skips that validation. Preserve native execution on the matching side. */
@Pseudo
@Mixin(targets = "com.tom.storagemod.StorageMod", remap = false)
public abstract class TomsDevelopmentDistMixin {
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target =
            "Lnet/minecraftforge/fml/DistExecutor;safeRunWhenOn(Lnet/minecraftforge/api/distmarker/Dist;Ljava/util/function/Supplier;)V"))
    private void digitalstorage$skipOtherSideValidation(Dist side,
            Supplier<DistExecutor.SafeRunnable> action, Operation<Void> original) {
        if (FMLEnvironment.dist == side) original.call(side, action);
    }
}
