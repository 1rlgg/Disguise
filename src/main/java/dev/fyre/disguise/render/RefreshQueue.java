package dev.fyre.disguise.render;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Main-thread-only FIFO for tab-list refresh work. Duplicates are coalesced and
 * capacity is bounded to prevent a burst of alias updates monopolizing a tick.
 */
public final class RefreshQueue {
    public static final class Pair {
        private final UUID viewer;
        private final UUID subject;

        public Pair(UUID viewer, UUID subject) {
            this.viewer = Objects.requireNonNull(viewer, "viewer");
            this.subject = Objects.requireNonNull(subject, "subject");
        }

        public UUID getViewer() { return viewer; }
        public UUID getSubject() { return subject; }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Pair)) return false;
            Pair that = (Pair) other;
            return viewer.equals(that.viewer) && subject.equals(that.subject);
        }

        @Override public int hashCode() { return 31 * viewer.hashCode() + subject.hashCode(); }
    }

    private final int capacity;
    private final LinkedHashSet<Pair> normal = new LinkedHashSet<Pair>();
    private final LinkedHashSet<Pair> urgent = new LinkedHashSet<Pair>();
    private final Set<Pair> queued = new HashSet<Pair>();
    private long droppedCosmetic;
    private long droppedRestoration;

    public RefreshQueue(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
    }

    /** Returns false only if capacity would be exceeded. Re-adding a pending pair is a no-op. */
    public boolean offer(UUID viewer, UUID subject) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(subject, "subject");
        if (viewer.equals(subject)) return true;
        Pair pair = new Pair(viewer, subject);
        if (queued.contains(pair)) return true;
        if (size() == capacity) {
            droppedCosmetic++;
            return false;
        }
        queued.add(pair);
        normal.add(pair);
        return true;
    }

    /**
     * Restoration is served before cosmetic updates. A queued cosmetic update
     * for the same pair is promoted, and saturation evicts a normal update
     * before sacrificing any pending restoration.
     */
    public boolean offerRestoration(UUID viewer, UUID subject) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(subject, "subject");
        if (viewer.equals(subject)) return true;
        Pair pair = new Pair(viewer, subject);
        if (urgent.contains(pair)) return true;
        if (queued.contains(pair)) {
            normal.remove(pair);
            urgent.add(pair);
                        return true;
        }
        if (size() >= capacity) {
            if (normal.isEmpty()) {
                // Never discard a restoration already waiting for delivery.
                // The applied-entry reconciler can retry this rejected pair
                // once capacity becomes available.
                droppedRestoration++;
                return false;
            }
            Pair evicted = takeLast(normal);
            queued.remove(evicted);
            droppedCosmetic++;
        }
        urgent.add(pair);
                queued.add(pair);
        return true;
    }

    private static Pair takeFirst(LinkedHashSet<Pair> entries) {
        Iterator<Pair> it = entries.iterator();
        Pair next = it.next();
        it.remove();
        return next;
    }

    private static Pair takeLast(LinkedHashSet<Pair> entries) {
        Iterator<Pair> it = entries.iterator();
        Pair last = null;
        while (it.hasNext()) last = it.next();
        entries.remove(last);
        return last;
    }

    public List<Pair> poll(int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        List<Pair> out = new ArrayList<Pair>();
        while (out.size() < limit && !queued.isEmpty()) {
            Pair pair = urgent.isEmpty() ? takeFirst(normal) : takeFirst(urgent);
                        queued.remove(pair);
            out.add(pair);
        }
        return out;
    }

    public void forget(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        normal.removeIf(pair -> pair.viewer.equals(playerId) || pair.subject.equals(playerId));
        urgent.removeIf(pair -> pair.viewer.equals(playerId) || pair.subject.equals(playerId));
                queued.removeIf(pair -> pair.viewer.equals(playerId) || pair.subject.equals(playerId));
    }

    public int size() { return queued.size(); }
    public int pendingRestorations() { return urgent.size(); }
    public long droppedCosmetic() { return droppedCosmetic; }
    public long droppedRestoration() { return droppedRestoration; }
    public void clear() {
        normal.clear(); urgent.clear(); queued.clear();
        droppedCosmetic = 0; droppedRestoration = 0;
    }
}
