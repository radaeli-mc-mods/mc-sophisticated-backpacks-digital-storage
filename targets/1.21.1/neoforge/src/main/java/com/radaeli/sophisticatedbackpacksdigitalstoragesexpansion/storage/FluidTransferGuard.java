package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Scoped guard for external fluid access to the backpack currently participating
 * in a remote Pump operation. Capability hooks should consult {@link #isBlocked}.
 */
public final class FluidTransferGuard implements AutoCloseable {
    private static final ThreadLocal<Map<UUID, Integer>> BLOCKED_CONTENTS = new ThreadLocal<>();

    private final UUID contentsId;
    private boolean closed;

    private FluidTransferGuard(UUID contentsId) {
        this.contentsId = contentsId;
        if (BLOCKED_CONTENTS.get() == null) {
            BLOCKED_CONTENTS.set(new HashMap<>());
        }
        BLOCKED_CONTENTS.get().merge(contentsId, 1, Integer::sum);
    }

    public static FluidTransferGuard blockExternalAccess(UUID contentsId) {
        if (contentsId == null) {
            throw new IllegalArgumentException("A fluid transfer guard requires a backpack contents UUID");
        }
        return new FluidTransferGuard(contentsId);
    }

    public static boolean isBlocked(UUID contentsId) {
        Map<UUID, Integer> blocked = BLOCKED_CONTENTS.get();
        return contentsId != null && blocked != null && blocked.containsKey(contentsId);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        Map<UUID, Integer> blocked = BLOCKED_CONTENTS.get();
        Integer count = blocked.get(contentsId);
        if (count == null || count <= 1) {
            blocked.remove(contentsId);
        } else {
            blocked.put(contentsId, count - 1);
        }
        if (blocked.isEmpty()) {
            BLOCKED_CONTENTS.remove();
        }
    }
}
