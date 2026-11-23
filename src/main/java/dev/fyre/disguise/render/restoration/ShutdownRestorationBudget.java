package dev.fyre.disguise.render.restoration;

import dev.fyre.disguise.render.RefreshQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Objects;

public final class ShutdownRestorationBudget {
    private final int limit;

    public ShutdownRestorationBudget(int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        this.limit = limit;
    }

    public List<RefreshQueue.Pair> select(Collection<RefreshQueue.Pair> applied) {
        Objects.requireNonNull(applied, "applied");
        List<RefreshQueue.Pair> result = new ArrayList<RefreshQueue.Pair>();
        for (RefreshQueue.Pair pair : applied) {
            if (result.size() >= limit) break;
            result.add(pair);
        }
        return Collections.unmodifiableList(result);
    }

    public List<RefreshQueue.Pair> selectPrioritized(Collection<RefreshQueue.Pair> applied,
                                                      Collection<RefreshQueue.Pair> urgent) {
        Objects.requireNonNull(applied, "applied");
        Objects.requireNonNull(urgent, "urgent");
        Set<RefreshQueue.Pair> eligible = new LinkedHashSet<RefreshQueue.Pair>(applied);
        LinkedHashSet<RefreshQueue.Pair> result = new LinkedHashSet<RefreshQueue.Pair>();
        for (RefreshQueue.Pair pair : urgent) {
            if (result.size() >= limit) break;
            if (eligible.contains(pair)) result.add(pair);
        }
        for (RefreshQueue.Pair pair : applied) {
            if (result.size() >= limit) break;
            result.add(pair);
        }
        return Collections.unmodifiableList(new ArrayList<RefreshQueue.Pair>(result));
    }

    public int omitted(int total) {
        if (total < 0) throw new IllegalArgumentException("total cannot be negative");
        return Math.max(0, total - limit);
    }
}
