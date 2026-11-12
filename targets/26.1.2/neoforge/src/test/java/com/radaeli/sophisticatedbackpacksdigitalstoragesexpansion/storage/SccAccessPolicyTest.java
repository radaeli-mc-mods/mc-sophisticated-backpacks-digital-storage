package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SccAccessPolicyTest {
    @Test void finiteCoverageIncludesBoundaryAtEveryNativeUpgradeCount() {
        for (int upgrades = 0; upgrades <= 8; upgrades++) {
            int range = 32 + upgrades * 32;
            double boundary = (double)range * range;
            assertTrue(SccCoverage.inRange(false, true, false, boundary, range));
            assertFalse(SccCoverage.inRange(false, true, false, Math.nextUp(boundary), range));
            assertFalse(SccCoverage.inRange(false, false, false, 0, range));
        }
    }
    @Test void infiniteIsRestrictedToSameDimension() {
        assertTrue(SccCoverage.inRange(false, true, true, Double.MAX_VALUE, 32));
        assertFalse(SccCoverage.inRange(false, false, true, 0, 32));
    }
    @Test void dimensionalOverridesDistanceAndDimensionWithoutInfinite() {
        assertTrue(SccCoverage.inRange(true, false, false, Double.MAX_VALUE, 32));
        assertTrue(SccCoverage.inRange(true, true, false, Double.MAX_VALUE, 32));
    }
}
