package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.List;
import java.util.function.Consumer;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.transfer.CheckpointedDelivery;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.transfer.DeliveryJournal;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Each fill settles its receipt before entering another independently committed handler. */
public final class FluidDelivery {
    private FluidDelivery() {}
    static CheckpointedDelivery.Step<FluidStack> step(String stage, IFluidHandler destination) {
        return new CheckpointedDelivery.Step<>(stage, offered -> {
            int accepted = destination.fill(offered.copy(), IFluidHandler.FluidAction.EXECUTE);
            if (accepted < 0 || accepted > offered.getAmount()) throw new IllegalStateException("Invalid fluid receipt at " + stage);
            var remainder = offered.copy(); remainder.shrink(accepted); return remainder;
        });
    }
    public static CheckpointedDelivery.Result<FluidStack> fill(IFluidHandler destination, FluidStack offered,
            Consumer<FluidStack> persist, Runnable inFlight) {
        return fill(destination, offered, persist, inFlight, "backend=unspecified, endpoint=unspecified");
    }
    public static CheckpointedDelivery.Result<FluidStack> fill(IFluidHandler destination, FluidStack offered,
            Consumer<FluidStack> persist, Runnable inFlight, String context) {
        var journal = new DeliveryJournal<FluidStack>() {
            private String deliveryStage = "fill";
            @Override public String description() { return context + ", stage=" + deliveryStage; }
            @Override public void checkpoint(FluidStack value) { persist.accept(value); }
            @Override public void beginDelivery(String stage) { deliveryStage = stage; inFlight.run(); }
        };
        var steps = destination instanceof PumpStorageRouter router ? router.deliverySteps()
                : List.of(step("external", destination));
        var result = CheckpointedDelivery.deliver(offered, steps, FluidStack::isEmpty, FluidStack::copy, journal);
        if (result.failure() != null) com.mojang.logging.LogUtils.getLogger().error(
                "Pump fill failed; {}, uncertain={}; last checkpoint retained without retry", journal.description(), result.uncertain(), result.failure());
        return result;
    }
}
