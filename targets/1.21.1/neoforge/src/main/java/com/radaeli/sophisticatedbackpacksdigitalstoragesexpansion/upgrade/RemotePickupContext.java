package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Player and entity context scoped to Sophisticated's native pickup callback.
 * The scope covers both its simulation and execution passes and supports reentrant pickups.
 */
public final class RemotePickupContext {
    private static final ThreadLocal<Deque<Context>> CONTEXTS = ThreadLocal.withInitial(ArrayDeque::new);

    private RemotePickupContext() {
    }

    public static Scope open(Player player, ItemEntity itemEntity) {
        Context context = new Context(player, itemEntity);
        CONTEXTS.get().push(context);
        return new Scope(context);
    }

    public static Optional<Context> current() {
        Deque<Context> contexts = CONTEXTS.get();
        if (contexts.isEmpty()) {
            CONTEXTS.remove();
            return Optional.empty();
        }
        Context context = contexts.peek();
        if (!context.isValid()) {
            return Optional.empty();
        }
        return Optional.of(context);
    }

    public record Context(Player player, ItemEntity itemEntity) {
        public boolean isValid() {
            return player != null && itemEntity != null
                    && !player.isRemoved() && !itemEntity.isRemoved()
                    && !player.level().isClientSide
                    && player.level() == itemEntity.level();
        }
    }

    public static final class Scope implements AutoCloseable {
        private final Context context;
        private boolean closed;

        private Scope(Context context) {
            this.context = context;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            Deque<Context> contexts = CONTEXTS.get();
            if (contexts.peek() != context) {
                contexts.clear();
                CONTEXTS.remove();
                throw new IllegalStateException("Pickup context scopes must close in reverse order");
            }
            contexts.pop();
            if (contexts.isEmpty()) {
                CONTEXTS.remove();
            }
        }
    }
}
