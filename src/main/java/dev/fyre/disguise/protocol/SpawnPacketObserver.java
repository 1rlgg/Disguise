package dev.fyre.disguise.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;

public final class SpawnPacketObserver extends PacketAdapter {
    private final TrackedSpawnPackets snapshots;
    private final ObservationCircuit observation = new ObservationCircuit();

    public SpawnPacketObserver(Plugin plugin, TrackedSpawnPackets snapshots) {
        super(plugin, ListenerPriority.MONITOR,
                PacketType.Play.Server.NAMED_ENTITY_SPAWN, PacketType.Play.Server.ENTITY_DESTROY);
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
    }

    @Override public void onPacketSending(PacketEvent event) {

        if (event.isCancelled() || !observation.enabled()) return;
        try {
            UUID viewer = event.getPlayer().getUniqueId();
            if (event.getPacketType() == PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
                UUID subject = event.getPacket().getUUIDs().read(0);
                Integer entityId = event.getPacket().getIntegers().read(0);
                if (subject != null && entityId != null) {
                    snapshots.capture(viewer, subject, entityId, event.getPacket());
                }
            } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_DESTROY) {
                snapshots.destroy(viewer, event.getPacket().getIntegerArrays().read(0));
            }
        } catch (RuntimeException ex) {
            if (observation.trip()) {

                snapshots.clear();
                getPlugin().getLogger().log(Level.WARNING,
                        "Spawn packet observation disabled after failure; using tab-list fallback", ex);
            }
        }
    }
    public static final class ObservationCircuit {
        private final java.util.concurrent.atomic.AtomicBoolean tripped =
                new java.util.concurrent.atomic.AtomicBoolean();

        public boolean enabled() { return !tripped.get(); }
        public boolean trip() { return tripped.compareAndSet(false, true); }
    }
}
