package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.gametest.mixin;

import net.minecraft.gametest.framework.GameTestMainUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Test-only: retain exactly the prepared test universe for the second server process. */
@Mixin(GameTestMainUtil.class)
public abstract class RestartWorldMixin {
    @Inject(method = "createOrResetDir(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void retainPreparedUniverse(String directory, CallbackInfo callback) {
        if (!"verify".equals(System.getProperty("digitalstorage.restartPhase", ""))) return;
        if (!java.nio.file.Files.isDirectory(java.nio.file.Path.of(directory).resolve("gametestworld"))) {
            throw new IllegalStateException("The restart verification has no prepared world");
        }
        callback.cancel();
    }
}
