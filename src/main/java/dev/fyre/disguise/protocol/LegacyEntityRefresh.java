package dev.fyre.disguise.protocol;

import dev.fyre.disguise.protocol.entity.EntityRefreshPolicy;

import com.comphenix.protocol.PacketType;
import dev.fyre.disguise.api.DisguiseService;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class LegacyEntityRefresh {
    private final ProtocolManager packets;
    private final PlayerInfoTabRefresh tab;
    private final TrackedSpawnPackets snapshots;
    private final DisguiseService disguises;

    public LegacyEntityRefresh(ProtocolManager packets, PlayerInfoTabRefresh tab,
                               TrackedSpawnPackets snapshots) {
        this(packets, tab, snapshots, null);
    }

    public LegacyEntityRefresh(ProtocolManager packets, PlayerInfoTabRefresh tab,
                               TrackedSpawnPackets snapshots, DisguiseService disguises) {
        this.packets = Objects.requireNonNull(packets, "packets");
        this.tab = Objects.requireNonNull(tab, "tab");
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
        this.disguises = disguises;
    }

    public boolean refresh(Player viewer, Player subject) throws InvocationTargetException {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Entity refresh must run on main thread");
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(subject, "subject");
        if (viewer.equals(subject) || !viewer.isOnline() || !subject.isOnline()) return false;
        if (!viewer.getWorld().equals(subject.getWorld())) return false;

        String alias = disguises == null ? null : disguises.getDisguiseFor(
                viewer.getUniqueId(), subject.getUniqueId())
                .map(disguise -> disguise.getName()).orElse(null);
        Player conflicting = alias == null ? null : Bukkit.getPlayerExact(alias);
        boolean aliasCollision = conflicting != null && conflicting.isOnline()
                && !conflicting.getUniqueId().equals(subject.getUniqueId());
        if (!aliasCollision && alias != null && disguises != null) {

            java.util.List<java.util.UUID> onlineIds = new java.util.ArrayList<java.util.UUID>();
            for (Player online : Bukkit.getOnlinePlayers()) onlineIds.add(online.getUniqueId());
            aliasCollision = EntityRefreshPolicy.conflicts(
                    disguises, viewer.getUniqueId(), subject.getUniqueId(), alias, onlineIds);
        }
        if (!EntityRefreshPolicy.mayRefreshEntity(
                viewer.getScoreboard().getEntryTeam(subject.getName()) != null,
                alias != null && viewer.getScoreboard().getEntryTeam(alias) != null,
                aliasCollision)) return false;

        TrackedSpawnPackets.Snapshot tracked = snapshots.lookup(viewer.getUniqueId(), subject.getUniqueId());
        boolean matchingSpawn = tracked != null && tracked.getEntityId() == subject.getEntityId();
        if (!EntityRefreshPolicy.permitted(true, viewer.canSee(subject), true,
                subject.isDead(), subject.isSleeping(), subject.isInsideVehicle() || subject.getPassenger() != null,
                "SPECTATOR".equals(subject.getGameMode().name())
                        || "SPECTATOR".equals(viewer.getGameMode().name()),
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

        ItemStack hand = subject.getItemInHand();
        spawn.getShorts().write(0, heldItemId(hand == null ? 0 : hand.getTypeId()));

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

            item.getShorts().write(0, equipmentSlot(slot));
            item.getItemModifier().write(0, equipment[slot]);
            gear[slot] = item;
        }

        tab.refresh(viewer, subject);

        EntityRefreshTransaction.execute(destroy, spawn, spawn.deepClone(), head,
                java.util.Arrays.asList(gear),
                packet -> packets.sendServerPacket(viewer, packet, false));
        return true;
    }

    public static short heldItemId(int id) {
        if (id < 0 || id > Short.MAX_VALUE) throw new IllegalArgumentException("Invalid legacy item ID");
        return (short) id;
    }

    public static short equipmentSlot(int slot) {
        if (slot < 0 || slot > 4) throw new IllegalArgumentException("Invalid 1.8 equipment slot");
        return (short) slot;
    }

    public static int fixed(double position) { return (int) Math.floor(position * 32.0D); }
    public static byte angle(float degrees) { return (byte) (int) (degrees * 256.0F / 360.0F); }
}
