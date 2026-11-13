package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManualControlsPreferenceTest {
    @Test
    void autoTracksWhetherAViewerAdapterCanServeTheMenu() {
        assertTrue(ManualControlsPreference.AUTO.isVisible(false));
        assertFalse(ManualControlsPreference.AUTO.isVisible(true));
    }

    @Test
    void explicitPreferencesOverrideViewerAvailability() {
        assertTrue(ManualControlsPreference.SHOW.isVisible(true));
        assertTrue(ManualControlsPreference.SHOW.isVisible(false));
        assertFalse(ManualControlsPreference.HIDE.isVisible(true));
        assertFalse(ManualControlsPreference.HIDE.isVisible(false));
    }

    @Test
    void toggleAlwaysPersistsAnExplicitChoice() {
        assertEquals(ManualControlsPreference.HIDE, ManualControlsPreference.AUTO.toggledFrom(true));
        assertEquals(ManualControlsPreference.SHOW, ManualControlsPreference.AUTO.toggledFrom(false));
    }
}
