package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.transfer;

/** Only a destination that can prove no mutation committed may report this failure. */
public final class UncommittedTransferException extends RuntimeException {
    public UncommittedTransferException(String message, Throwable cause) {
        super(message, cause);
    }
}
