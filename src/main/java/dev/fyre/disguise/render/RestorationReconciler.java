package dev.fyre.disguise.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Round-robin bounded scan of tab entries actually applied to clients.
 * Reconciles restoration work lost during queue saturation without creating
 * unbounded scheduled jobs or client packets.
 */
public final class RestorationReconciler {
    private int offset;

    public List<RefreshQueue.Pair> scan(Collection<RefreshQueue.Pair> applied,
                                        Predicate<RefreshQueue.Pair> needsRestore, int maxInspect) {
        Objects.requireNonNull(applied, "applied");
        Objects.requireNonNull(needsRestore, "needsRestore");
        if (maxInspect < 1) throw new IllegalArgumentException("maxInspect must be positive");
        List<RefreshQueue.Pair> entries = new ArrayList<RefreshQueue.Pair>(applied);
        List<RefreshQueue.Pair> restore = new ArrayList<RefreshQueue.Pair>();
        if (entries.isEmpty()) {
            offset = 0;
            return restore;
        }
        int inspected = Math.min(maxInspect, entries.size());
        int from = offset % entries.size();
        for (int i = 0; i < inspected; i++) {
            RefreshQueue.Pair pair = entries.get((from + i) % entries.size());
            if (needsRestore.test(pair)) restore.add(pair);
        }
        offset = (from + inspected) % entries.size();
        return restore;
    }

    public void reset() { offset = 0; }
}
