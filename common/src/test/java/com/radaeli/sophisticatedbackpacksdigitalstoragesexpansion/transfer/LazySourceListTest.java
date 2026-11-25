package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.transfer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LazySourceListTest {
    @Test void successfulPrimaryNeverQueriesTheFallbackEvenThroughAStream() {
        var calls = new AtomicInteger();
        var sources = new LazySourceList<>(() -> List.of(1, 2), () -> { calls.incrementAndGet(); return List.of(3); });
        assertEquals(1, sources.stream().findFirst().orElseThrow());
        assertEquals(0, calls.get());
    }
    @Test void unavailablePrimaryCandidatesStillReachEveryAlternativeInStableOrder() {
        var calls = new AtomicInteger();
        var sources = new LazySourceList<>(() -> List.of(1, 2), () -> { calls.incrementAndGet(); return List.of(3, 4); });
        assertEquals(List.of(1, 2, 3, 4), sources.stream().toList());
        assertEquals(List.of(1, 2, 3, 4), sources.stream().toList());
        assertEquals(1, calls.get());
    }
}
