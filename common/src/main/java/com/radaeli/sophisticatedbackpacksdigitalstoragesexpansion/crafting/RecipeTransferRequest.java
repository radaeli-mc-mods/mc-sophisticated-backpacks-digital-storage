package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting;

import java.util.Objects;

/** Internal recipe-viewer transfer intent. It deliberately contains no viewer API types. */
public record RecipeTransferRequest(String recipeId, Mode mode) {
    public RecipeTransferRequest {
        Objects.requireNonNull(recipeId, "recipeId");
        Objects.requireNonNull(mode, "mode");
    }

    public enum Mode {
        SINGLE,
        MAX
    }
}
