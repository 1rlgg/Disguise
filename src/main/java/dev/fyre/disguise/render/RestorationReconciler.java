package dev.fyre.disguise.render;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;


public final class RestorationReconciler {
    private int offset;

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
}
