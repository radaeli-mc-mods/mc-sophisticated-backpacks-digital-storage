package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting.IngredientAssignment.Candidate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngredientAssignmentTest {
    @Test
    void reassignsAPreferredIngredientToMakeRoomForARestrictedFallback() {
        // Candidate 0 serves both slots. Candidate 1 only serves slot 0.
        var stock = List.of(new Candidate(0b11, 1), new Candidate(0b01, 1));
        assertArrayEquals(new int[] {1, 0}, IngredientAssignment.match(2, stock, 0).orElseThrow());
    }

    @Test
    void preservesExistingShapelessIngredientsAndUsesThePreferredSource() {
        var stock = List.of(new Candidate(0b011, 1), new Candidate(0b101, 2), new Candidate(0b111, 3));
        assertArrayEquals(new int[] {1, 0, 1}, IngredientAssignment.match(3, stock, 1).orElseThrow());
    }

    @Test
    void rejectsAnExistingIngredientThatCannotFitAnywhere() {
        var stock = List.of(new Candidate(0b01, 1), new Candidate(0b01, 1), new Candidate(0b10, 1));
        assertTrue(IngredientAssignment.match(2, stock, 2).isEmpty());
    }

    @Test
    void returnsTheMaximumPartialFillWithoutCountingAUnitTwice() {
        var stock = List.of(new Candidate(0b011, 1), new Candidate(0b100, 1));
        assertArrayEquals(new int[] {0, -1, 1}, IngredientAssignment.match(3, stock, 0).orElseThrow());
    }
}
