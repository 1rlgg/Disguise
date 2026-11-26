package gg.rl1.disguise.render;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

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
    private long droppedCosmetic;
    private long droppedRestoration;

    public RefreshQueue(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
    }

    public boolean offer(UUID viewer, UUID subject) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(subject, "subject");
        if (viewer.equals(subject)) return true;
        return offer(new Pair(viewer, subject));
    }

    public boolean offer(Pair pair) {
        Objects.requireNonNull(pair, "pair");
        if (pair.viewer.equals(pair.subject)) return true;
        if (contains(pair)) return true;
        if (size() >= capacity) {
            droppedCosmetic++;
            return false;
        }
        normal.add(pair);
        return true;
    }

    public boolean offerRestoration(UUID viewer, UUID subject) {
        return offerRestoration(viewer, subject, evicted -> { });
    }

    public boolean offerRestoration(UUID viewer, UUID subject,
                                    Consumer<Pair> onCosmeticEvicted) {
        Objects.requireNonNull(onCosmeticEvicted, "onCosmeticEvicted");
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(subject, "subject");
        if (viewer.equals(subject)) return true;
        Pair pair = new Pair(viewer, subject);
        if (urgent.contains(pair)) return true;
        if (normal.remove(pair)) {
            urgent.add(pair);
                        return true;
        }
        if (size() >= capacity) {
            if (normal.isEmpty()) {

                droppedRestoration++;
                return false;
            }

            Pair evicted = takeFirst(normal);
            droppedCosmetic++;
            urgent.add(pair);
            onCosmeticEvicted.accept(evicted);
            return true;
        }
        urgent.add(pair);
        return true;
    }

    private static Pair takeFirst(LinkedHashSet<Pair> entries) {
        Iterator<Pair> it = entries.iterator();
        Pair next = it.next();
        it.remove();
        return next;
    }

    public List<Pair> poll(int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        if (size() == 0) return Collections.emptyList();
        List<Pair> out = new ArrayList<Pair>(Math.min(limit, size()));
        drain(urgent, out, limit);
        drain(normal, out, limit);
        return out;
    }

    private static void drain(LinkedHashSet<Pair> entries, List<Pair> out, int limit) {
        if (entries.isEmpty() || out.size() >= limit) return;
        Iterator<Pair> iterator = entries.iterator();
        while (iterator.hasNext() && out.size() < limit) {
            out.add(iterator.next());
            iterator.remove();
        }
    }

    public void forget(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        normal.removeIf(pair -> pair.viewer.equals(playerId) || pair.subject.equals(playerId));
        urgent.removeIf(pair -> pair.viewer.equals(playerId) || pair.subject.equals(playerId));
    }

    public int size() { return normal.size() + urgent.size(); }
    public boolean hasFreeSlot() { return size() < capacity; }
    public boolean contains(UUID viewer, UUID subject) {
        return contains(new Pair(viewer, subject));
    }

    public boolean contains(Pair pair) {
        Objects.requireNonNull(pair, "pair");
        return normal.contains(pair) || urgent.contains(pair);
    }
    public int pendingRestorations() { return urgent.size(); }

    public List<Pair> pendingRestorationSnapshot() {
        return Collections.unmodifiableList(new ArrayList<Pair>(urgent));
    }
    public long droppedCosmetic() { return droppedCosmetic; }
    public long droppedRestoration() { return droppedRestoration; }
    public void clear() {
        normal.clear(); urgent.clear();
        droppedCosmetic = 0; droppedRestoration = 0;
    }
}
