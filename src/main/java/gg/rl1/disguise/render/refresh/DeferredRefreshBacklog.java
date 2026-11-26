package gg.rl1.disguise.render.refresh;

import gg.rl1.disguise.render.RefreshQueue;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.UUID;

public final class DeferredRefreshBacklog {
    private final int capacity;
    private final LinkedHashSet<RefreshQueue.Pair> deferred = new LinkedHashSet<RefreshQueue.Pair>();
    private long overflow;

    public DeferredRefreshBacklog(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
    }

    public boolean offer(UUID viewer, UUID subject) {
        return offer(new RefreshQueue.Pair(viewer, subject));
    }

    public boolean offer(RefreshQueue.Pair pair) {
        Objects.requireNonNull(pair, "pair");
        if (pair.getViewer().equals(pair.getSubject()) || deferred.contains(pair)) return true;
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
        int inspected = 0;
        Iterator<RefreshQueue.Pair> iter = deferred.iterator();
        while (iter.hasNext() && inspected++ < maxPerTick) {
            RefreshQueue.Pair pair = iter.next();
            if (queue.contains(pair)) {
                iter.remove();
                continue;
            }
            if (!queue.hasFreeSlot() || !queue.offer(pair)) break;
            iter.remove();
            moved++;
        }
        return moved;
    }

    public boolean cancel(UUID viewer, UUID subject) {
        return cancel(new RefreshQueue.Pair(viewer, subject));
    }

    public boolean cancel(RefreshQueue.Pair pair) {
        return deferred.remove(Objects.requireNonNull(pair, "pair"));
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
