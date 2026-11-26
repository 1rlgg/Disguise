package gg.rl1.disguise.protocol.spawn;

import com.comphenix.protocol.events.PacketContainer;
import gg.rl1.disguise.render.RefreshQueue;
import java.util.Iterator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public final class TrackedSpawnPackets {
    public static final class Snapshot {
        private final int entityId;
        private final PacketContainer packet;
        private final long capturedAt;
        private Snapshot(int entityId, PacketContainer packet, long capturedAt) {
            this.entityId = entityId;
            this.packet = packet;
            this.capturedAt = capturedAt;
        }
        public int getEntityId() { return entityId; }
        public PacketContainer getPacket() { return packet.deepClone(); }
    }

    private final int capacity;
    private final long ttlNanos;
    private final LongSupplier clock;
    private final LinkedHashMap<RefreshQueue.Pair, Snapshot> cache =
            new LinkedHashMap<RefreshQueue.Pair, Snapshot>(16, 0.75f, true);

    private final Map<UUID, Map<RefreshQueue.Pair, Integer>> byViewer = new HashMap<>();

    public TrackedSpawnPackets(int capacity) {
        this(capacity, TimeUnit.MINUTES.toNanos(2), System::nanoTime);
    }

    public TrackedSpawnPackets(int capacity, long ttlNanos, LongSupplier clock) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
        if (ttlNanos < 1) throw new IllegalArgumentException("ttl must be positive");
        this.ttlNanos = ttlNanos;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public static boolean expiredSince(long captured, long ttl, LongSupplier clock) {
        if (ttl < 1) throw new IllegalArgumentException("ttl must be positive");
        return Objects.requireNonNull(clock, "clock").getAsLong() - captured >= ttl;
    }

    public synchronized void capture(UUID viewer, UUID subject, int entityId, PacketContainer packet) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(packet, "packet");
        if (viewer.equals(subject)) return;
        RefreshQueue.Pair key = new RefreshQueue.Pair(viewer, subject);
        cache.put(key, new Snapshot(entityId, packet.deepClone(), clock.getAsLong()));
        byViewer.computeIfAbsent(viewer, ignored -> new HashMap<>()).put(key, entityId);
        while (cache.size() > capacity) {
            Iterator<Map.Entry<RefreshQueue.Pair, Snapshot>> iterator = cache.entrySet().iterator();
            RefreshQueue.Pair evicted = iterator.next().getKey();
            iterator.remove();
            unindex(evicted);
        }
    }

    public synchronized Snapshot lookup(UUID viewer, UUID subject) {
        RefreshQueue.Pair key = new RefreshQueue.Pair(viewer, subject);
        Snapshot entry = cache.get(key);
        if (entry == null) return null;
        if (expiredSince(entry.capturedAt, ttlNanos, clock)) {
            cache.remove(key);
            unindex(key);
            return null;
        }
        return entry;
    }

    public synchronized void destroy(UUID viewer, int[] entityIds) {
        Objects.requireNonNull(viewer, "viewer");
        if (entityIds == null || entityIds.length == 0) return;
        Map<RefreshQueue.Pair, Integer> keys = byViewer.get(viewer);
        if (keys == null) return;
        Set<Integer> ids = null;
        if (entityIds.length > 8) {
            ids = new HashSet<>();
            for (int id : entityIds) ids.add(id);
        }
        Iterator<Map.Entry<RefreshQueue.Pair, Integer>> iterator = keys.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<RefreshQueue.Pair, Integer> entry = iterator.next();
            int entityId = entry.getValue();
            if (ids == null ? containsEntity(entityIds, entityId) : ids.contains(entityId)) {
                cache.remove(entry.getKey());
                iterator.remove();
            }
        }
        if (keys.isEmpty()) byViewer.remove(viewer);
    }

    private static boolean containsEntity(int[] ids, int entityId) {
        for (int id : ids) if (id == entityId) return true;
        return false;
    }

    private void unindex(RefreshQueue.Pair key) {
        Map<RefreshQueue.Pair, Integer> keys = byViewer.get(key.getViewer());
        if (keys != null) {
            keys.remove(key);
            if (keys.isEmpty()) byViewer.remove(key.getViewer());
        }
    }

    public synchronized void forget(UUID playerId) {
        Iterator<RefreshQueue.Pair> iterator = cache.keySet().iterator();
        while (iterator.hasNext()) {
            RefreshQueue.Pair key = iterator.next();
            if (key.getViewer().equals(playerId) || key.getSubject().equals(playerId)) {
                iterator.remove();
                unindex(key);
            }
        }
    }

    public synchronized int purgeExpired() {
        long now = clock.getAsLong();
        int removed = 0;
        Iterator<Map.Entry<RefreshQueue.Pair, Snapshot>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<RefreshQueue.Pair, Snapshot> entry = iterator.next();
            if (now - entry.getValue().capturedAt >= ttlNanos) {
                RefreshQueue.Pair key = entry.getKey();
                iterator.remove();
                unindex(key);
                removed++;
            }
        }
        return removed;
    }

    public synchronized boolean isEmpty() { return cache.isEmpty(); }
    public synchronized int size() { return cache.size(); }
    public synchronized void clear() { cache.clear(); byViewer.clear(); }
}
