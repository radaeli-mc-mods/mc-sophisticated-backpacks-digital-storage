package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients;
import org.cyclops.integrateddynamics.api.part.PartPos;

/** Tracks only disables acquired by this native call, preserving pre-existing and outer leases. */
public final class IntegratedPositionScope implements AutoCloseable {
    private static final ThreadLocal<Deque<IntegratedPositionScope>> SCOPES = ThreadLocal.withInitial(ArrayDeque::new);
    private final Map<IPositionedAddonsNetworkIngredients<?, ?>, Set<PartPos>> owned = new IdentityHashMap<>();
    private IntegratedPositionScope() { SCOPES.get().push(this); }
    public static IntegratedPositionScope open() { return new IntegratedPositionScope(); }
    public static void acquired(IPositionedAddonsNetworkIngredients<?, ?> network, PartPos pos) {
        var scopes = SCOPES.get();
        if (!scopes.isEmpty()) scopes.peek().owned.computeIfAbsent(network, ignored -> new LinkedHashSet<>()).add(pos);
        else SCOPES.remove();
    }
    public static void released(IPositionedAddonsNetworkIngredients<?, ?> network, PartPos pos) {
        var scopes = SCOPES.get();
        if (!scopes.isEmpty()) {
            var positions = scopes.peek().owned.get(network);
            if (positions != null) positions.remove(pos);
        } else SCOPES.remove();
    }
    @Override public void close() {
        try {
            owned.forEach((network, positions) -> positions.forEach(pos -> {
                if (network.isPositionDisabled(pos)) network.enablePosition(pos);
            }));
        } finally {
            var scopes = SCOPES.get();
            if (scopes.pop() != this) throw new IllegalStateException("Unbalanced native position scopes");
            if (scopes.isEmpty()) SCOPES.remove();
        }
    }
}
