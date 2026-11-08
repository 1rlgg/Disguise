package dev.fyre.disguise.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * EXPERIMENTAL Minecraft 1.8.8/1.8.9 live client entity refresh.
 * Replays an actual captured named-player spawn with the original entity ID
 * and UUID, after updating tab-list profile data. Does not modify the Bukkit
 * entity, world tracking, hitbox, server combat or vanish state.
 * Client-side visual flicker and incompatibilities remain possible.
 */
public final class LegacyEntityRefresh {
    private final ProtocolManager packets;
    private final PlayerInfoTabRefresh tab;
    private final TrackedSpawnPackets snapshots;

    public LegacyEntityRefresh(ProtocolManager packets, PlayerInfoTabRefresh tab,
                               TrackedSpawnPackets snapshots) {
        this.packets = Objects.requireNonNull(packets, "packets");
        this.tab = Objects.requireNonNull(tab, "tab");
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
    }

    /**
     * Returns true if we had a tracked spawn to refresh. False means the caller
     * may still refresh tab info, but should not synthesize an entity spawn.
     */
    public boolean refresh(Player viewer, Player subject) throws InvocationTargetException {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Entity refresh must run on main thread");
        if (viewer.equals(subject) || !viewer.isOnline() || !subject.isOnline()
                || !viewer.canSee(subject) || !viewer.getWorld().equals(subject.getWorld())) return false;
        TrackedSpawnPackets.Snapshot tracked = snapshots.lookup(viewer.getUniqueId(), subject.getUniqueId());
        if (tracked == null || tracked.getEntityId() != subject.getEntityId()) return false;

        PacketContainer spawn = tracked.getPacket();
        Location loc = subject.getLocation();
        // Minecraft 1.8 uses fixed-point integer coordinates and one-byte rotation.
        spawn.getIntegers().write(1, fixed(loc.getX()));
        spawn.getIntegers().write(2, fixed(loc.getY()));
        spawn.getIntegers().write(3, fixed(loc.getZ()));
        spawn.getBytes().write(0, angle(loc.getYaw()));
        spawn.getBytes().write(1, angle(loc.getPitch()));
        spawn.getDataWatcherModifier().write(0, WrappedDataWatcher.getEntityWatcher(subject));

        // Prepare all packets before deleting anything from the client.
        PacketContainer destroy = packets.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        destroy.getIntegerArrays().write(0, new int[]{tracked.getEntityId()});
        PacketContainer head = packets.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        head.getIntegers().write(0, tracked.getEntityId());
        head.getBytes().write(0, angle(loc.getYaw()));

        ItemStack[] equipment = new ItemStack[]{
                subject.getItemInHand(), subject.getInventory().getBoots(),
                subject.getInventory().getLeggings(), subject.getInventory().getChestplate(),
                subject.getInventory().getHelmet()};
        PacketContainer[] gear = new PacketContainer[5];
        for (int slot = 0; slot < equipment.length; slot++) {
            PacketContainer packet = packets.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
            packet.getIntegers().write(0, tracked.getEntityId());
            packet.getIntegers().write(1, slot);
            packet.getItemModifier().write(0, equipment[slot]);
            gear[slot] = packet;
        }

        // The profile must arrive before the fresh named-player spawn.
        tab.refresh(viewer, subject);
        packets.sendServerPacket(viewer, destroy, false);
        try {
            packets.sendServerPacket(viewer, spawn, false);
        } catch (InvocationTargetException | RuntimeException failure) {
            // Best-effort recovery with original server-produced spawn snapshot.
            try {
                packets.sendServerPacket(viewer, tracked.getPacket(), false);
            } catch (InvocationTargetException | RuntimeException recoverFailure) {
                failure.addSuppressed(recoverFailure);
            }
            throw failure;
        }
        packets.sendServerPacket(viewer, head, false);
        for (PacketContainer item : gear) packets.sendServerPacket(viewer, item, false);
        return true;
    }

    public static int fixed(double position) {
        return (int) Math.floor(position * 32.0D);
    }

    public static byte angle(float degrees) {
        return (byte) (int) (degrees * 256.0F / 360.0F);
    }
}
