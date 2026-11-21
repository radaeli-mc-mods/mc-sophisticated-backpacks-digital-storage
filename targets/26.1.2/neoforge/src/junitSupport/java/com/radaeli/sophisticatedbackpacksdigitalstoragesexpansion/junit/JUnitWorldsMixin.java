package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.junit;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The official ephemeral server omits worlds; Cyclops initializes SavedData on server start. */
@Mixin(MinecraftServer.class)
public abstract class JUnitWorldsMixin {
    @Shadow protected abstract void createLevels();

    @Inject(method = "runServer", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/server/ServerLifecycleHooks;handleServerStarted(Lnet/minecraft/server/MinecraftServer;)V"))
    private void initializeJUnitWorlds(CallbackInfo callback) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        if (server.getClass().getName().equals("net.neoforged.testframework.junit.EphemeralTestServerProvider$JUnitServer")
                && server.getLevel(Level.OVERWORLD) == null) createLevels();
    }
}
