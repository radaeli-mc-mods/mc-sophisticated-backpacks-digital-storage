package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.Set;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.IMagnetPreventionChecker;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.mixin.MagnetUpgradeWrapperAccessor;

/** Applies the live Sophisticated magnet prevention registry to addon candidates. */
public final class RemoteMagnetEligibility {
    private RemoteMagnetEligibility() {
    }

    public static boolean isBlocked(Entity entity) {
        Set<IMagnetPreventionChecker> checkers = MagnetUpgradeWrapperAccessor
                .sophisticatedBackpackIsAddon$getMagnetCheckers();
        for (IMagnetPreventionChecker checker : checkers) {
            if (checker.isBlocked(entity)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInNativeCooldown(Level level) {
        return MagnetUpgradeWrapperAccessor.sophisticatedBackpackIsAddon$getNextTickTime() > level.getGameTime();
    }
}
