package dev.fyre.disguise.render;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.UUID;

/**
 * Main-thread-only bounded secondary backlog for cosmetic refreshes rejected
 * by a saturated primary queue. Never evicts queued restorations.
 */
public final class DeferredRefreshBacklog {
    private final int capacity;
    private final LinkedHashSet<RefreshQueue.Pair> deferred = new LinkedHashSet<RefreshQueue.Pair>();
    private long overflow;

    public DeferredRefreshBacklog(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
    }

    public boolean offer(UUID viewer, UUID subject) {
        RefreshQueue.Pair pair = new RefreshQueue.Pair(viewer, subject);
        if (viewer.equals(subject) || deferred.contains(pair)) return true;
        if (deferred.size() >= capacity) {
            overflow++;
            return false;
        }
        return deferred.add(pair);
    }

    public int drainTo(RefreshQueue queue, int maxPerTick) {
        Objects.requireNonNull(queue, "queue");
        if (maxPerTick < 1) throw new IllegalArgumentException("maxPerTick must be positive");
        int moved = 0;
        Iterator<RefreshQueue.Pair> iter = deferred.iterator();
        while (iter.hasNext() && moved < maxPerTick && queue.hasFreeSlot()) {
            RefreshQueue.Pair pair = iter.next();
            if (!queue.offer(pair.getViewer(), pair.getSubject())) break;
            iter.remove();
            moved++;
        }
        return moved;
    }

    public void forget(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        deferred.removeIf(pair -> playerId.equals(pair.getViewer())
                || playerId.equals(pair.getSubject()));
    }

    public int size() { return deferred.size(); }
    public long overflowCount() { return overflow; }
    public void clear() { deferred.clear(); overflow = 0; }
}
