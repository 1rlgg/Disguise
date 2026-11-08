package dev.fyre.disguise.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Prevent synchronous packet storms when a plugin is disabling.
 * Any unattempted identities MUST be reported; this is not client recovery.
 */
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

    public int omitted(int total) {
        if (total < 0) throw new IllegalArgumentException("total cannot be negative");
        return Math.max(0, total - limit);
    }
}
