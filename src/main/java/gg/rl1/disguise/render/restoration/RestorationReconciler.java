package gg.rl1.disguise.render.restoration;

import gg.rl1.disguise.render.RefreshQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public final class RestorationReconciler {
    private int offset;
    private final int limit;

    public RestorationReconciler() { this(256); }

    public RestorationReconciler(int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        this.limit = limit;
    }

    public List<RefreshQueue.Pair> scan(Collection<RefreshQueue.Pair> applied,
                                        Predicate<RefreshQueue.Pair> needsRestore, int maxInspect) {
        Objects.requireNonNull(applied, "applied");
        Objects.requireNonNull(needsRestore, "needsRestore");
        if (maxInspect < 1) throw new IllegalArgumentException("maxInspect must be positive");
        List<RefreshQueue.Pair> restore = new ArrayList<RefreshQueue.Pair>();
        if (applied.isEmpty()) {
            offset = 0;
            return restore;
        }

        int inspected = Math.min(maxInspect, applied.size());
        int from = offset % applied.size();
        if (applied instanceof java.util.RandomAccess && applied instanceof List) {
            List<RefreshQueue.Pair> indexed = (List<RefreshQueue.Pair>) applied;
            for (int i = 0; i < inspected; i++) {
                RefreshQueue.Pair pair = indexed.get((from + i) % indexed.size());
                if (needsRestore.test(pair)) restore.add(pair);
            }
            offset = (from + inspected) % applied.size();
            return restore;
        }
        Iterator<RefreshQueue.Pair> iter = applied.iterator();
        for (int skip = 0; skip < from; skip++) iter.next();
        for (int i = 0; i < inspected; i++) {
            if (!iter.hasNext()) iter = applied.iterator();
            RefreshQueue.Pair pair = iter.next();
            if (needsRestore.test(pair)) restore.add(pair);
        }
        offset = (from + inspected) % applied.size();
        return restore;
    }

    public void reset() { offset = 0; }

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
        Collection<RefreshQueue.Pair> eligible = applied instanceof Set
                ? applied : new LinkedHashSet<RefreshQueue.Pair>(applied);
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
