package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Capacitated matching for a crafting grid. Candidates are ordered by source preference. */
public final class IngredientAssignment {
    private IngredientAssignment() {
    }

    public static Optional<int[]> match(int slots, List<Candidate> candidates, int requiredCandidates) {
        int[] owners = new int[slots];
        Arrays.fill(owners, -1);
        for (int candidate = 0; candidate < candidates.size(); candidate++) {
            for (int unit = 0; unit < candidates.get(candidate).capacity(); unit++) {
                if (!assign(candidate, candidates, owners, new boolean[slots])) {
                    if (candidate < requiredCandidates) {
                        return Optional.empty();
                    }
                    break;
                }
            }
        }
        return Optional.of(owners);
    }

    private static boolean assign(int candidate, List<Candidate> candidates, int[] owners, boolean[] visited) {
        int compatible = candidates.get(candidate).compatibleSlots();
        for (int slot = 0; slot < owners.length; slot++) {
            if ((compatible & (1 << slot)) == 0 || visited[slot] || owners[slot] == candidate) {
                continue;
            }
            visited[slot] = true;
            // Moving an earlier assignment preserves its units while making room for this one.
            if (owners[slot] == -1 || assign(owners[slot], candidates, owners, visited)) {
                owners[slot] = candidate;
                return true;
            }
        }
        return false;
    }

    public record Candidate(int compatibleSlots, int capacity) {
    }
}
