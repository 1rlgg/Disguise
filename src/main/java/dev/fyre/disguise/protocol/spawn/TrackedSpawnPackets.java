package dev.fyre.disguise.protocol.spawn;

import com.comphenix.protocol.events.PacketContainer;
import dev.fyre.disguise.render.RefreshQueue;
import java.util.Iterator;
import java.util.HashSet;
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
        cache.put(new RefreshQueue.Pair(viewer, subject), new Snapshot(entityId, packet.deepClone(), clock.getAsLong()));
        while (cache.size() > capacity) {
            Iterator<Map.Entry<RefreshQueue.Pair, Snapshot>> iterator = cache.entrySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    public synchronized Snapshot lookup(UUID viewer, UUID subject) {
        RefreshQueue.Pair key = new RefreshQueue.Pair(viewer, subject);
        Snapshot entry = cache.get(key);
        if (entry == null) return null;
        if (expiredSince(entry.capturedAt, ttlNanos, clock)) {
            cache.remove(key);
            return null;
        }
        return entry;
    }

    public synchronized void destroy(UUID viewer, int[] entityIds) {
        Objects.requireNonNull(viewer, "viewer");
        if (entityIds == null) return;
        if (entityIds.length == 0) return;
        if (entityIds.length == 1) {
            int id = entityIds[0];
            cache.entrySet().removeIf(entry -> entry.getKey().getViewer().equals(viewer)
                    && entry.getValue().entityId == id);
            return;
        }
        Set<Integer> ids = new HashSet<Integer>();
        for (int id : entityIds) ids.add(id);
        cache.entrySet().removeIf(entry -> entry.getKey().getViewer().equals(viewer)
                && ids.contains(entry.getValue().entityId));
    }

    public synchronized void forget(UUID playerId) {
        cache.keySet().removeIf(key -> key.getViewer().equals(playerId) || key.getSubject().equals(playerId));
    }

    public synchronized int size() { return cache.size(); }
    public synchronized void clear() { cache.clear(); }
}
