package dev.fyre.disguise.protocol;

import com.comphenix.protocol.events.PacketContainer;
import dev.fyre.disguise.render.RefreshQueue;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Thread-safe bounded collection of actual server-produced player spawn packets.
 * Never invents entity IDs, UUIDs or metadata; callers receive deep copies.
 */
public final class TrackedSpawnPackets {
    public static final class Snapshot {
        private final int entityId;
        private final PacketContainer packet;
        private Snapshot(int entityId, PacketContainer packet) {
            this.entityId = entityId;
            this.packet = packet;
        }
        public int getEntityId() { return entityId; }
        public PacketContainer getPacket() { return packet.deepClone(); }
    }

    private final int capacity;
    private final LinkedHashMap<RefreshQueue.Pair, Snapshot> cache =
            new LinkedHashMap<RefreshQueue.Pair, Snapshot>(16, 0.75f, true);

    public TrackedSpawnPackets(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
    }

    public synchronized void capture(UUID viewer, UUID subject, int entityId, PacketContainer packet) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(packet, "packet");
        if (viewer.equals(subject)) return;
        cache.put(new RefreshQueue.Pair(viewer, subject), new Snapshot(entityId, packet.deepClone()));
        while (cache.size() > capacity) {
            Iterator<Map.Entry<RefreshQueue.Pair, Snapshot>> iterator = cache.entrySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    public synchronized Snapshot lookup(UUID viewer, UUID subject) {
        Snapshot entry = cache.get(new RefreshQueue.Pair(viewer, subject));
        return entry == null ? null : new Snapshot(entry.entityId, entry.packet.deepClone());
    }

    public synchronized void destroy(UUID viewer, int[] entityIds) {
        Objects.requireNonNull(viewer, "viewer");
        if (entityIds == null) return;
        cache.entrySet().removeIf(entry -> {
            if (!entry.getKey().getViewer().equals(viewer)) return false;
            for (int id : entityIds) if (id == entry.getValue().entityId) return true;
            return false;
        });
    }

    public synchronized void forget(UUID playerId) {
        cache.keySet().removeIf(key -> key.getViewer().equals(playerId) || key.getSubject().equals(playerId));
    }

    public synchronized int size() { return cache.size(); }
    public synchronized void clear() { cache.clear(); }
}
