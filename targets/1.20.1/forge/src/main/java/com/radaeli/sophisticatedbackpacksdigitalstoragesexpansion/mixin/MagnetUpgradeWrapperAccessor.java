package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.mixin;

import java.util.Set;

import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.IMagnetPreventionChecker;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = MagnetUpgradeWrapper.class, remap = false)
public interface MagnetUpgradeWrapperAccessor {
    @Accessor(value = "nextTickTime", remap = false)
    static long sophisticatedBackpackIsAddon$getNextTickTime() {
        throw new AssertionError("Mixin accessor is applied at runtime");
    }

    @Accessor(value = "magnetCheckers", remap = false)
    static Set<IMagnetPreventionChecker> sophisticatedBackpackIsAddon$getMagnetCheckers() {
        throw new AssertionError("Mixin accessor is applied at runtime");
    }
}
