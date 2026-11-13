package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting;

/** Client preference for the recipe selector and manual ingredient button. */
public enum ManualControlsPreference {
    AUTO,
    SHOW,
    HIDE;

    public boolean isVisible(boolean recipeViewerReady) {
        return switch (this) {
            case AUTO -> !recipeViewerReady;
            case SHOW -> true;
            case HIDE -> false;
        };
    }

    public ManualControlsPreference toggledFrom(boolean currentlyVisible) {
        return currentlyVisible ? HIDE : SHOW;
    }
}
