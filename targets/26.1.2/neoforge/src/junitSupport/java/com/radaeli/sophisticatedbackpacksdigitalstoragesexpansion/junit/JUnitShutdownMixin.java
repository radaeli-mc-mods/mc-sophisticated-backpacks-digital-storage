package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.junit;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Join the ephemeral server before FML closes its classloader and deletes its world. */
@Mixin(targets = "net.neoforged.testframework.junit.EphemeralTestServerProvider$JUnitServer", remap = false)
public abstract class JUnitShutdownMixin {
    @Inject(method = "stopServer", at = @At("HEAD"), cancellable = true)
    private void stopOnServerThread(CallbackInfo callback) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        if (Thread.currentThread() != server.getRunningThread()) {
            server.halt(true);
            callback.cancel();
        }
    }
}
