package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Operation-local routing; both sources must provide the same fluid and components. */
public final class PumpStorageRouter implements IFluidHandler {
    private final IFluidHandler remote;
    private final List<IFluidHandler> ordered;
    private boolean usedRemoteExtraction;
    private Consumer<FluidStack> extractionListener = ignored -> {};

    public PumpStorageRouter(IFluidHandler remote, IFluidHandler local, boolean networkFirst) {
        this.remote = remote;
        List<IFluidHandler> handlers = new ArrayList<>(2);
        if (networkFirst && remote != null) handlers.add(remote);
        if (local != null) handlers.add(local);
        if (!networkFirst && remote != null) handlers.add(remote);
        ordered = List.copyOf(handlers);
    }

    public boolean isAvailable() { return !ordered.isEmpty(); }
    public boolean hasRemote() { return remote != null; }
    public boolean usedRemoteExtraction() { return usedRemoteExtraction; }

    public void onExtraction(Consumer<FluidStack> listener) { extractionListener = listener; }

    @Override
    public int getTanks() {
        return ordered.stream().mapToInt(IFluidHandler::getTanks).sum();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (tank < 0) return FluidStack.EMPTY;
        for (IFluidHandler handler : ordered) {
            if (tank < handler.getTanks()) return handler.getFluidInTank(tank).copy();
            tank -= handler.getTanks();
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        if (tank < 0) return 0;
        for (IFluidHandler handler : ordered) {
            if (tank < handler.getTanks()) return handler.getTankCapacity(tank);
            tank -= handler.getTanks();
        }
        return 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (tank < 0) return false;
        for (IFluidHandler handler : ordered) {
            if (tank < handler.getTanks()) return handler.isFluidValid(tank, stack);
            tank -= handler.getTanks();
        }
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        int requested = Math.min(resource.getAmount(), PumpFluidHandlerFacade.MAX_OPERATION_MB);
        int accepted = 0;
        for (IFluidHandler handler : ordered) {
            if (accepted >= requested) break;
            FluidStack offer = com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.FluidStacks.copyWithAmount(resource, requested - accepted);
            accepted += Math.max(0, Math.min(offer.getAmount(), handler.fill(offer, action)));
        }
        return accepted;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;
        int requested = Math.min(resource.getAmount(), PumpFluidHandlerFacade.MAX_OPERATION_MB);
        int extracted = 0;
        if (action.execute()) usedRemoteExtraction = false;
        for (IFluidHandler handler : ordered) {
            if (extracted >= requested) break;
            FluidStack part = handler.drain(com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.FluidStacks.copyWithAmount(resource, requested - extracted), action);
            if (part.isEmpty()) continue;
            if (!resource.isFluidEqual(part) || part.getAmount() > requested - extracted) {
                throw new IllegalStateException("Pump source returned a different fluid or exceeded its offer");
            }
            extracted += part.getAmount();
            if (action.execute() && handler == remote) usedRemoteExtraction = true;
            // Persist the known amount before invoking a second storage backend.
            if (action.execute()) extractionListener.accept(com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.FluidStacks.copyWithAmount(resource, extracted));
        }
        return extracted == 0 ? FluidStack.EMPTY : com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.FluidStacks.copyWithAmount(resource, extracted);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) return FluidStack.EMPTY;
        for (IFluidHandler handler : ordered) {
            FluidStack candidate = handler.drain(Math.min(maxDrain, PumpFluidHandlerFacade.MAX_OPERATION_MB),
                    FluidAction.SIMULATE);
            if (!candidate.isEmpty()) return drain(com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.FluidStacks.copyWithAmount(candidate, maxDrain), action);
        }
        return FluidStack.EMPTY;
    }
}
