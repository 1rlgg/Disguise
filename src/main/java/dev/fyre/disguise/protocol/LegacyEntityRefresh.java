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
 * EXPERIMENTAL Minecraft 1.8.8/1.8.9 client-side destroy and respawn.
 * No Bukkit entity, hitbox, server combat, or visibility is changed.
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

    public boolean refresh(Player viewer, Player subject) throws InvocationTargetException {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Entity refresh must run on main thread");
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(subject, "subject");
        if (viewer.equals(subject) || !viewer.isOnline() || !subject.isOnline()) return false;
        if (!viewer.getWorld().equals(subject.getWorld())) return false;

        TrackedSpawnPackets.Snapshot tracked = snapshots.lookup(viewer.getUniqueId(), subject.getUniqueId());
        boolean matchingSpawn = tracked != null && tracked.getEntityId() == subject.getEntityId();
        if (!EntityRefreshPolicy.permitted(true, viewer.canSee(subject), true,
                subject.isDead(), subject.isSleeping(), subject.isInsideVehicle() || !subject.getPassengers().isEmpty(),
                EntityRefreshPolicy.withinTrackingRange(viewer.getLocation().distanceSquared(subject.getLocation()),
                        Bukkit.getViewDistance()), matchingSpawn)) return false;

        PacketContainer spawn = tracked.getPacket();
        Location loc = subject.getLocation();
        spawn.getIntegers().write(1, fixed(loc.getX()));
        spawn.getIntegers().write(2, fixed(loc.getY()));
        spawn.getIntegers().write(3, fixed(loc.getZ()));
        spawn.getBytes().write(0, angle(loc.getYaw()));
        spawn.getBytes().write(1, angle(loc.getPitch()));
        spawn.getDataWatcherModifier().write(0, WrappedDataWatcher.getEntityWatcher(subject));

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
            PacketContainer item = packets.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
            item.getIntegers().write(0, tracked.getEntityId());
            item.getIntegers().write(1, slot);
            item.getItemModifier().write(0, equipment[slot]);
            gear[slot] = item;
        }

        tab.refresh(viewer, subject);
        packets.sendServerPacket(viewer, destroy, false);
        try {
            packets.sendServerPacket(viewer, spawn, false);
        } catch (InvocationTargetException | RuntimeException ex) {
            try {
                packets.sendServerPacket(viewer, tracked.getPacket(), false);
            } catch (InvocationTargetException | RuntimeException recovery) {
                ex.addSuppressed(recovery);
            }
            throw ex;
        }
        packets.sendServerPacket(viewer, head, false);
        for (PacketContainer item : gear) packets.sendServerPacket(viewer, item, false);
        return true;
    }

    public static int fixed(double position) { return (int) Math.floor(position * 32.0D); }
    public static byte angle(float degrees) { return (byte) (int) (degrees * 256.0F / 360.0F); }
}
