package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.List;
import java.util.Objects;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Operation-scoped view of linked fluid storage for Pump's native IFluidHandler
 * routines. It is deliberately bounded to one bucket and must never be attached
 * as the backpack's public fluid capability.
 */
public final class PumpFluidHandlerFacade implements IFluidHandler {
    public static final int MAX_OPERATION_MB = 1_000;

    public enum Direction {
        HANDLER_TO_NETWORK,
        NETWORK_TO_HANDLER
    }

    private final FluidStorageTarget target;
    private final ServerPlayer player;
    private final Direction direction;
    private final List<FluidStorageAmount> availableSnapshot;
    private int remainingMb = MAX_OPERATION_MB;

    public PumpFluidHandlerFacade(FluidStorageTarget target, ServerPlayer player, Direction direction) {
        this.target = Objects.requireNonNull(target, "target");
        this.player = Objects.requireNonNull(player, "player");
        this.direction = Objects.requireNonNull(direction, "direction");
        availableSnapshot = direction == Direction.NETWORK_TO_HANDLER
                ? List.copyOf(target.getAvailableFluids(player))
                : List.of();
    }

    @Override
    public int getTanks() {
        return direction == Direction.NETWORK_TO_HANDLER ? Math.max(1, availableSnapshot.size()) : 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (tank < 0 || tank >= getTanks() || direction != Direction.NETWORK_TO_HANDLER || remainingMb <= 0) {
            return FluidStack.EMPTY;
        }
        if (tank >= availableSnapshot.size()) {
            return FluidStack.EMPTY;
        }
        FluidStorageAmount amount = availableSnapshot.get(tank);
        return amount.key().toStack((int) Math.min(remainingMb, amount.amountMb()));
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank >= 0 && tank < getTanks() ? MAX_OPERATION_MB : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (tank != 0 || stack.isEmpty() || remainingMb <= 0) {
            return false;
        }
        return direction == Direction.HANDLER_TO_NETWORK
                && target.simulateInsert(stack.copyWithAmount(Math.min(stack.getAmount(), remainingMb)), player) > 0;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (direction != Direction.HANDLER_TO_NETWORK || resource.isEmpty() || remainingMb <= 0) {
            return 0;
        }
        FluidStack offered = resource.copyWithAmount(Math.min(resource.getAmount(), remainingMb));
        if (action.simulate()) {
            return (int) Math.min(offered.getAmount(), target.simulateInsert(offered, player));
        }
        FluidStack remainder = target.insert(offered, player);
        int accepted = offered.getAmount() - (remainder.isEmpty() ? 0 : remainder.getAmount());
        remainingMb -= accepted;
        return accepted;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (direction != Direction.NETWORK_TO_HANDLER || resource.isEmpty() || remainingMb <= 0) {
            return FluidStack.EMPTY;
        }
        FluidStorageKey key = FluidStorageKey.of(resource);
        long requested = Math.min(resource.getAmount(), remainingMb);
        if (action.simulate()) {
            long amount = target.simulateExtract(key, requested, player);
            return key.toStack((int) Math.min(requested, amount));
        }
        FluidStack extracted = target.extract(key, requested, player);
        remainingMb = Math.max(0, remainingMb - extracted.getAmount());
        return extracted;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (direction != Direction.NETWORK_TO_HANDLER || maxDrain <= 0 || remainingMb <= 0) {
            return FluidStack.EMPTY;
        }
        for (FluidStorageAmount candidate : availableSnapshot) {
            FluidStack extracted = drain(candidate.key().toStack(Math.min(maxDrain, remainingMb)), action);
            if (!extracted.isEmpty()) {
                return extracted;
            }
        }
        return FluidStack.EMPTY;
    }
}
