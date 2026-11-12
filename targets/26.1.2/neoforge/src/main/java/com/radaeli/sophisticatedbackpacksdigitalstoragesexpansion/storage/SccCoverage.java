package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

/** Position-independent projection of SCC 1.5.3 WirelessAccess.inRange. */
final class SccCoverage {
    private SccCoverage() {}

    static boolean inRange(boolean dimensional, boolean sameDimension, boolean infinite,
            double distanceSquared, int range) {
        return dimensional || (sameDimension && (infinite || distanceSquared <= (double) range * range));
    }
}
