package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.HashMap;
import java.util.Map;

import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;

/** Scoped protection against a network inserting back into its source through Sophisticated's public IO. */
public final class StorageTransferGuard implements AutoCloseable {
    private static final ThreadLocal<Map<GuardKey, Integer>> BLOCKED = new ThreadLocal<>();
    private final Object storageId;
    private final Direction direction;
    private final Thread owner = Thread.currentThread();
    private boolean closed;

    private StorageTransferGuard(IStorageWrapper storage, Direction direction) {
        storageId = identity(storage);
        this.direction = direction;
        Map<GuardKey, Integer> blocked = BLOCKED.get();
        if (blocked == null) {
            blocked = new HashMap<>();
            BLOCKED.set(blocked);
        }
        blocked.merge(new GuardKey(storageId, direction), 1, Integer::sum);
    }

    public static StorageTransferGuard blockExternalInsertion(IStorageWrapper storage) {
        return new StorageTransferGuard(storage, Direction.INPUT);
    }

    public static StorageTransferGuard blockExternalExtraction(IStorageWrapper storage) {
        return new StorageTransferGuard(storage, Direction.OUTPUT);
    }

    public static boolean isExternalInsertionBlocked(IStorageWrapper storage) {
        return isBlocked(storage, Direction.INPUT);
    }

    public static boolean isExternalExtractionBlocked(IStorageWrapper storage) {
        return isBlocked(storage, Direction.OUTPUT);
    }

    private static boolean isBlocked(IStorageWrapper storage, Direction direction) {
        Map<GuardKey, Integer> blocked = BLOCKED.get();
        Object storageId = identity(storage);
        return blocked != null && (blocked.containsKey(new GuardKey(storageId, direction))
                || blocked.containsKey(new GuardKey(storage, direction)));
    }

    private static Object identity(IStorageWrapper storage) {
        // Linked wrappers can represent the same inventory. Prefer the native contents identity over object identity.
        return storage.getContentsUuid().<Object>map(uuid -> uuid).orElse(storage);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        if (Thread.currentThread() != owner) {
            throw new IllegalStateException("A storage transfer scope must close on the thread that opened it");
        }
        closed = true;
        Map<GuardKey, Integer> blocked = BLOCKED.get();
        GuardKey key = new GuardKey(storageId, direction);
        blocked.computeIfPresent(key, (ignored, count) -> count == 1 ? null : count - 1);
        if (blocked.isEmpty()) {
            BLOCKED.remove();
        }
    }

    private enum Direction { INPUT, OUTPUT }
    private record GuardKey(Object storage, Direction direction) {}
}
