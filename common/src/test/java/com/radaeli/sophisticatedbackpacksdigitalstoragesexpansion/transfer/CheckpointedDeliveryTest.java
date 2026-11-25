package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.transfer;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CheckpointedDeliveryTest {
    @Test void onlyAProvenUncommittedFailureMayReturnSpendableCustody() {
        var journal = new Journal();
        var result = CheckpointedDelivery.deliver(64, List.of(
                new CheckpointedDelivery.Step<Integer>("first", offer -> offer - 16),
                new CheckpointedDelivery.Step<Integer>("second", offer -> {
                    assertEquals(48, journal.pending);
                    assertTrue(journal.uncertain);
                    throw new UncommittedTransferException("proven abort", null);
                })), amount -> amount == 0, amount -> amount, journal);
        assertFalse(result.uncertain());
        assertFalse(journal.uncertain);
        assertEquals(48, result.remainder());
    }
    private static class Journal implements DeliveryJournal<Integer> {
        int pending;
        boolean uncertain;
        @Override public void checkpoint(Integer value) { pending = value; uncertain = false; }
        @Override public void beginDelivery(String stage) { uncertain = true; }
    }
}
