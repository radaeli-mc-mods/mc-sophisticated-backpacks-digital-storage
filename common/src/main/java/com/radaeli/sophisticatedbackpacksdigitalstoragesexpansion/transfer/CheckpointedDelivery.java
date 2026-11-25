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
            journal.beginDelivery(step.stage());
            T next;
            try {
                next = step.deliver().apply(copy.apply(remaining));
            } catch (UncommittedTransferException failure) {
                journal.checkpoint(copy.apply(remaining));
                return new Result<>(remaining, false, failure);
            } catch (RuntimeException failure) {
                return new Result<>(remaining, true, failure);
            }
            remaining = copy.apply(next);
            journal.checkpoint(copy.apply(remaining));
        }
        return new Result<>(remaining, false, null);
    }
}
