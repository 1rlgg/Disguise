package dev.fyre.disguise.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Main-thread-only bookkeeping for tab entries this plugin successfully refreshed. */
public final class AppliedTabEntries {
    private final Set<RefreshQueue.Pair> applied = new HashSet<RefreshQueue.Pair>();

    public void applied(UUID viewer, UUID subject) {
        applied.add(new RefreshQueue.Pair(viewer, subject));
    }

    public boolean contains(UUID viewer, UUID subject) {
        return applied.contains(new RefreshQueue.Pair(viewer, subject));
    }

    public void restored(UUID viewer, UUID subject) {
        applied.remove(new RefreshQueue.Pair(viewer, subject));
    }

    public void forget(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        applied.removeIf(pair -> pair.getViewer().equals(playerId) || pair.getSubject().equals(playerId));
    }

    public Set<RefreshQueue.Pair> snapshot() {
        return Collections.unmodifiableSet(new HashSet<RefreshQueue.Pair>(applied));
    }

    public int size() { return applied.size(); }
    public void clear() { applied.clear(); }
}
