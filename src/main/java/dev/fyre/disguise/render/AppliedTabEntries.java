package dev.fyre.disguise.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;


public final class AppliedTabEntries {
    private final Set<RefreshQueue.Pair> applied = new LinkedHashSet<RefreshQueue.Pair>();
    private Set<RefreshQueue.Pair> cachedSnapshot;

    public void applied(UUID viewer, UUID subject) {
        if (applied.add(new RefreshQueue.Pair(viewer, subject))) cachedSnapshot = null;
    }

    public boolean contains(UUID viewer, UUID subject) {
        return applied.contains(new RefreshQueue.Pair(viewer, subject));
    }

    public void restored(UUID viewer, UUID subject) {
        if (applied.remove(new RefreshQueue.Pair(viewer, subject))) cachedSnapshot = null;
    }

    public void forget(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        if (applied.removeIf(pair -> pair.getViewer().equals(playerId) || pair.getSubject().equals(playerId))) {
            cachedSnapshot = null;
        }
    }

    public Set<RefreshQueue.Pair> snapshot() {


        if (cachedSnapshot == null) {
            cachedSnapshot = Collections.unmodifiableSet(new LinkedHashSet<RefreshQueue.Pair>(applied));
        }
        return cachedSnapshot;
    }

    public int size() { return applied.size(); }
    public void clear() { applied.clear(); cachedSnapshot = null; }
}
