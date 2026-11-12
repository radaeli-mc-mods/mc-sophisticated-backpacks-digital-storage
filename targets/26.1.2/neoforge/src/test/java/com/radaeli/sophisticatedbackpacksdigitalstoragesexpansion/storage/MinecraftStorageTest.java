package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;

public abstract class MinecraftStorageTest {
    @BeforeAll
    public static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        net.neoforged.testframework.junit.EphemeralTestServerProvider.grabServer();
    }
}
