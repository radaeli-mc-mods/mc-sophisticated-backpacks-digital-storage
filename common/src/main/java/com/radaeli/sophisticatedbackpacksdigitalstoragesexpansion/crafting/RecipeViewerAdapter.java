package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting;

/** Internal adapter contract used by the client UI to check an active viewer handler. */
public interface RecipeViewerAdapter {
    String id();

    /** True only when this adapter's transfer handler is active for the current menu. */
    boolean isReadyFor(Object menu);
}
