package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import java.util.function.BooleanSupplier;

import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.SourcePriority;

/** Compact source-priority toggle shared by linked-storage upgrade tabs. */
final class SourcePriorityControl {
    private SourcePriorityControl() {
    }

    static ToggleButton<SourcePriority> create(Position position, BooleanSupplier networkFirst, Runnable onClick) {
        return RemoteSourcePriorityButton.create(position,
                () -> networkFirst.getAsBoolean() ? SourcePriority.NETWORK_FIRST : SourcePriority.BACKPACK_FIRST,
                onClick);
    }
}
