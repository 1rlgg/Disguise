package dev.fyre.disguise.render;

import java.util.ArrayDeque;
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
    private final ArrayDeque<Pair> queue = new ArrayDeque<Pair>();
    private final Set<Pair> queued = new HashSet<Pair>();

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
        if (queue.size() == capacity) return false;
        queued.add(pair);
        queue.addLast(pair);
        return true;
    }

    public List<Pair> poll(int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        List<Pair> out = new ArrayList<Pair>();
        while (out.size() < limit && !queue.isEmpty()) {
            Pair pair = queue.removeFirst();
            queued.remove(pair);
            out.add(pair);
        }
        return out;
    }

    public void forget(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        queue.removeIf(pair -> pair.viewer.equals(playerId) || pair.subject.equals(playerId));
        queued.removeIf(pair -> pair.viewer.equals(playerId) || pair.subject.equals(playerId));
    }

    public int size() { return queue.size(); }
    public void clear() { queue.clear(); queued.clear(); }
}
