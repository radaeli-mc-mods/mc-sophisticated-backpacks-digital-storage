package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Bounded, component-preserving transfers with persisted ownership of every extracted amount. */
public final class PumpTransferService {
    private PumpTransferService() {}

    public static List<FluidStack> candidates(IFluidHandler source, int maximum, Predicate<FluidStack> filter) {
        int limit = Math.min(maximum, PumpFluidHandlerFacade.MAX_OPERATION_MB);
        List<FluidStack> candidates = new ArrayList<>();
        Set<FluidStorageKey> seen = new HashSet<>();
        if (limit <= 0) return candidates;
        // Some source capabilities implement drain(int) without exposing their contents as tanks.
        addCandidate(candidates, seen, source.drain(limit, IFluidHandler.FluidAction.SIMULATE), filter);
        for (int tank = 0; tank < source.getTanks(); tank++) {
            FluidStack contents = source.getFluidInTank(tank);
            if (contents.isEmpty()) continue;
            FluidStack candidate = source.drain(com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.FluidStacks.copyWithAmount(contents, limit), IFluidHandler.FluidAction.SIMULATE);
            addCandidate(candidates, seen, candidate, filter);
        }
        return candidates;
    }

    private static void addCandidate(List<FluidStack> result, Set<FluidStorageKey> seen, FluidStack candidate,
            Predicate<FluidStack> filter) {
        if (!candidate.isEmpty() && filter.test(candidate) && seen.add(FluidStorageKey.of(candidate))) {
            result.add(candidate.copy());
        }
    }

    /** Returns true once the source changed, even if delivery leaves a pending reservation. */
    public static boolean transfer(IFluidHandler source, IFluidHandler destination, int maximum, int minimum,
            Predicate<FluidStack> filter, Consumer<FluidStack> persist, boolean restoreSource) {
        for (FluidStack candidate : candidates(source, maximum, filter)) {
            int accepted = Math.max(0, Math.min(candidate.getAmount(), destination.fill(candidate.copy(), IFluidHandler.FluidAction.SIMULATE)));
            if (accepted < minimum) continue;
            FluidStack extracted = source.drain(com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.FluidStacks.copyWithAmount(candidate, accepted), IFluidHandler.FluidAction.EXECUTE);
            if (extracted.isEmpty()) continue;
            persist.accept(extracted.copy());
            int inserted = Math.max(0, Math.min(extracted.getAmount(), destination.fill(extracted.copy(), IFluidHandler.FluidAction.EXECUTE)));
            FluidStack remainder = com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.FluidStacks.copyWithAmount(extracted, extracted.getAmount() - inserted);
            persist.accept(remainder.copy());
            if (restoreSource && !remainder.isEmpty()) {
                int restored = Math.max(0, Math.min(remainder.getAmount(), source.fill(remainder.copy(), IFluidHandler.FluidAction.EXECUTE)));
                remainder.shrink(restored);
                persist.accept(remainder.copy());
            }
            return true;
        }
        return false;
    }
}
