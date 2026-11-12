package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.mixin;

import java.util.Set;

import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.IMagnetPreventionChecker;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MagnetUpgradeWrapper.class)
public interface MagnetUpgradeWrapperAccessor {
    @Accessor("nextTickTime")
    static long sophisticatedBackpackIsAddon$getNextTickTime() {
        throw new AssertionError("Mixin accessor is applied at runtime");
    }

    @Accessor("magnetCheckers")
    static Set<IMagnetPreventionChecker> sophisticatedBackpackIsAddon$getMagnetCheckers() {
        throw new AssertionError("Mixin accessor is applied at runtime");
    }
}
