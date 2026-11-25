package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.transfer;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/** Operation-local progress; an unknown result retains custody and stops every following destination. */
public final class CheckpointedDelivery {
    private CheckpointedDelivery() {}

    public record Step<T>(String stage, UnaryOperator<T> deliver) {}
    public record Result<T>(T remainder, boolean uncertain, RuntimeException failure) {}

    public static <T> Result<T> deliver(T offered, List<Step<T>> steps, Predicate<T> empty,
            UnaryOperator<T> copy, DeliveryJournal<T> journal) {
        T remaining = copy.apply(offered);
        journal.checkpoint(copy.apply(remaining));
        for (Step<T> step : steps) {
            if (empty.test(remaining)) break;
            try {
                journal.beginDelivery(step.stage());
            } catch (RuntimeException failure) {
                return quarantine(journal, remaining, step.stage(), failure);
            }
            T next;
            try {
                next = step.deliver().apply(copy.apply(remaining));
            } catch (UncommittedTransferException failure) {
                try {
                    journal.checkpoint(copy.apply(remaining));
                } catch (RuntimeException checkpointFailure) {
                    if (checkpointFailure != failure) failure.addSuppressed(checkpointFailure);
                    return quarantine(journal, remaining, step.stage(), failure);
                }
                return new Result<>(remaining, false, failure);
            } catch (RuntimeException failure) {
                return new Result<>(remaining, true, failure);
            }
            remaining = copy.apply(next);
            try {
                journal.checkpoint(copy.apply(remaining));
            } catch (RuntimeException failure) {
                return quarantine(journal, remaining, step.stage(), failure);
            }
        }
        return new Result<>(remaining, false, null);
    }

    private static <T> Result<T> quarantine(DeliveryJournal<T> journal, T remaining, String stage,
            RuntimeException failure) {
        // A failed save after an immediate delivery must not escape the native source update.
        // Re-mark in-flight in memory even when the save callback itself keeps failing.
        try {
            journal.beginDelivery(stage + ".checkpoint");
        } catch (RuntimeException markerFailure) {
            if (markerFailure != failure) failure.addSuppressed(markerFailure);
        }
        return new Result<>(remaining, true, failure);
    }
}
