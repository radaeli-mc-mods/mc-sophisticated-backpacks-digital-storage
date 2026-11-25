package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.transfer;

/** Persisted custody: checkpoint a known remainder and mark each immediate call before entering it. */
public interface DeliveryJournal<T> {
    void checkpoint(T remainder);
    void beginDelivery(String stage);
}
