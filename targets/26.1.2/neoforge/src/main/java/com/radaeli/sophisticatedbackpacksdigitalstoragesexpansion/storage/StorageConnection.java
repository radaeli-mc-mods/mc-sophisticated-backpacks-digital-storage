package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Objects;
import java.util.Optional;

/** Result of a single server-side access check; never persisted or cached on an upgrade. */
public record StorageConnection(Status status, Optional<StorageTarget> target) {
    public StorageConnection {
        Objects.requireNonNull(status);
        Objects.requireNonNull(target);
        if ((status == Status.CONNECTED) != target.isPresent()) {
            throw new IllegalArgumentException("Only a connected result may contain a storage target");
        }
    }

    public static StorageConnection connected(StorageTarget target) {
        return new StorageConnection(Status.CONNECTED, Optional.of(target));
    }

    public static StorageConnection unavailable(Status status) {
        return new StorageConnection(status, Optional.empty());
    }

    public enum Status {
        CONNECTED("connected"),
        NOT_LINKED("not_linked"),
        BACKEND_MISSING("backend_missing"),
        DIMENSION_UNAVAILABLE("dimension_unavailable"),
        ENDPOINT_UNLOADED("endpoint_unloaded"),
        ENDPOINT_MISSING("endpoint_missing"),
        ACCESS_DENIED("access_denied"),
        UNAVAILABLE("unavailable");

        private final String messageKey;

        Status(String messageKey) {
            this.messageKey = "message.sophisticated_backpacks_digital_storages_expansion." + messageKey;
        }

        public String messageKey() {
            return messageKey;
        }
    }
}
