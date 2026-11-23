package dev.fyre.disguise.render.restoration;

import dev.fyre.disguise.render.RefreshQueue;

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
        applied(new RefreshQueue.Pair(viewer, subject));
    }

    public void applied(RefreshQueue.Pair pair) {
        if (applied.add(Objects.requireNonNull(pair, "pair"))) cachedSnapshot = null;
    }

    public boolean contains(UUID viewer, UUID subject) {
        return contains(new RefreshQueue.Pair(viewer, subject));
    }

    public boolean contains(RefreshQueue.Pair pair) {
        return applied.contains(Objects.requireNonNull(pair, "pair"));
    }

    public void restored(UUID viewer, UUID subject) {
        restored(new RefreshQueue.Pair(viewer, subject));
    }

    public void restored(RefreshQueue.Pair pair) {
        if (applied.remove(Objects.requireNonNull(pair, "pair"))) cachedSnapshot = null;
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
