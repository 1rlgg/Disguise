package dev.fyre.disguise;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.command.DisguiseDiagnostics;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import dev.fyre.disguise.api.DisguiseService;
import dev.fyre.disguise.command.DisguiseCommand;
import dev.fyre.disguise.command.SkinCommandHandler;
import dev.fyre.disguise.core.InMemoryDisguiseService;
import dev.fyre.disguise.core.ObservableDisguiseService;
import dev.fyre.disguise.protocol.LegacyEntityRefresh;
import dev.fyre.disguise.protocol.packet.PlayerInfoPacketListener;
import dev.fyre.disguise.protocol.packet.PlayerInfoRewriter;
import dev.fyre.disguise.protocol.packet.PlayerInfoTabRefresh;
import dev.fyre.disguise.protocol.spawn.SpawnPacketObserver;
import dev.fyre.disguise.protocol.spawn.TrackedSpawnPackets;
import dev.fyre.disguise.render.identity.IdentityRenderer;
import dev.fyre.disguise.render.identity.LoggedIdentityRenderer;
import dev.fyre.disguise.render.RefreshQueue;
import dev.fyre.disguise.render.RefreshTargetPlanner;
import dev.fyre.disguise.render.TabListIdentityRenderer;
import dev.fyre.disguise.skin.AsyncSkinService;
import dev.fyre.disguise.skin.MojangSkinSource;
import dev.fyre.disguise.skin.cache.SkinCache;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import dev.fyre.disguise.protocol.entity.EntityRefreshPolicy;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class FyreDisguisePlugin extends JavaPlugin implements Listener {
    private DisguiseService disguises;
    private InMemoryDisguiseService registry;
    private AsyncSkinService skins;
    private SkinCommandHandler skinCommands;
    private ProtocolManager packets;
    private TabListIdentityRenderer tabRenderer;
    private RefreshTargetPlanner refreshPlanner;
    private TrackedSpawnPackets spawnPackets;
    private final ResyncTickets resyncTickets = new ResyncTickets();

    @Override public void onEnable() {
        saveDefaultConfig();
        registry = new InMemoryDisguiseService();
        packets = ProtocolLibrary.getProtocolManager();
        boolean experimentalRefresh = getConfig().getBoolean("experimental-tab-refresh", false);
        boolean entityRefresh = getConfig().getBoolean("experimental-entity-refresh", false);
        if (entityRefresh && !experimentalRefresh) {
            getLogger().warning("Entity refresh requires experimental-tab-refresh: true; disabling entity refresh");
            entityRefresh = false;
        }
        if (experimentalRefresh) {
            PlayerInfoTabRefresh tab = new PlayerInfoTabRefresh(packets, registry);
            LegacyEntityRefresh world = null;
            if (entityRefresh) {
                spawnPackets = new TrackedSpawnPackets(4096);
                packets.addPacketListener(new SpawnPacketObserver(this, spawnPackets));
                world = new LegacyEntityRefresh(packets, tab, spawnPackets, registry);
            }
            tabRenderer = new TabListIdentityRenderer(tab, getLogger(), 2048, registry, world);
            refreshPlanner = new RefreshTargetPlanner(registry);
        }
        IdentityRenderer renderer = experimentalRefresh ? tabRenderer : new LoggedIdentityRenderer(getLogger());
        disguises = new ObservableDisguiseService(registry, renderer, this::onlineViewerIds, this::assertMainThread);
        skins = new AsyncSkinService(new MojangSkinSource(3000, 5000),
                new SkinCache(512, TimeUnit.MINUTES.toNanos(30)), 2, 128);
        skinCommands = new SkinCommandHandler(this, disguises, skins);
        getCommand("fyredisguise").setExecutor(new DisguiseCommand(disguises,
                skinCommands, () -> new dev.fyre.disguise.command.DisguiseDiagnostics(
                        tabRenderer != null, spawnPackets != null,
                        tabRenderer == null ? 0 : tabRenderer.pendingCount(),
                        tabRenderer == null ? 0 : tabRenderer.appliedCount(),
                        spawnPackets == null ? 0 : spawnPackets.size(),
                        skins == null ? 0 : skins.pendingLookups(),
                        tabRenderer == null ? 0 : tabRenderer.pendingRestorationCount(),
                        tabRenderer == null ? 0 : tabRenderer.droppedCosmeticCount(),
                        tabRenderer == null ? 0 : tabRenderer.displacedRestorationCount(),
                        tabRenderer != null && tabRenderer.entityRefreshCircuitOpen(),
                        tabRenderer == null ? 0 : tabRenderer.retryCount(),
                        tabRenderer == null ? 0 : tabRenderer.exhaustedRetryCount(),
                        tabRenderer == null ? 0 : tabRenderer.rejectedRetryCount(),
                        skins == null ? 0 : skins.coolingDownSources(),
                        tabRenderer == null ? 0 : tabRenderer.measuredFlushes(),
                        tabRenderer == null ? 0 : tabRenderer.measuredPairs(),
                        tabRenderer == null ? 0 : tabRenderer.averageFlushMicros(),
                        tabRenderer == null ? 0 : tabRenderer.peakFlushMicros(),
                        tabRenderer == null ? 0 : tabRenderer.deferredCount(),
                        tabRenderer == null ? 0 : tabRenderer.deferredOverflowCount()).lines(),
                (viewer, subject) -> tabRenderer != null
                        && tabRenderer.requestOriginalRestoration(viewer, subject)));
        packets.addPacketListener(new PlayerInfoPacketListener(this, new PlayerInfoRewriter(registry)));
        Bukkit.getPluginManager().registerEvents(this, this);
        if (tabRenderer != null) Bukkit.getScheduler().runTaskTimer(this, () -> tabRenderer.flush(32), 1L, 1L);
        getLogger().info("Tab refresh: " + experimentalRefresh + "; EXPERIMENTAL entity refresh: " + entityRefresh);
    }

    private Collection<UUID> onlineViewerIds() {
        Collection<UUID> ids = new ArrayList<UUID>();
        for (Player player : Bukkit.getOnlinePlayers()) ids.add(player.getUniqueId());
        return ids;
    }

    private void assertMainThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Disguise mutations require the primary server thread");
        if (!isEnabled() || disguises == null) {
            throw new IllegalStateException("Disguise mutations require an enabled plugin");
        }
    }

    private void scheduleResync(UUID playerId) {
        if (tabRenderer == null) return;
        final long ticket = resyncTickets.schedule(playerId);
        Bukkit.getScheduler().runTaskLater(this, () -> {

            if (!resyncTickets.consume(playerId, ticket)) return;
            if (tabRenderer == null || refreshPlanner == null || !isEnabled()) return;
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) return;
            for (RefreshQueue.Pair pair : refreshPlanner.around(playerId, onlineViewerIds(),
                    candidate -> tabRenderer.wasApplied(candidate.getViewer(), candidate.getSubject()))) {
                tabRenderer.enqueue(pair.getViewer(), pair.getSubject());
            }
        }, 2L);
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) { scheduleResync(event.getPlayer().getUniqueId()); }
    @EventHandler public void onRespawn(PlayerRespawnEvent event) {
        invalidateSpawns(event.getPlayer().getUniqueId());
        scheduleResync(event.getPlayer().getUniqueId());
    }
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent event) {
        invalidateSpawns(event.getPlayer().getUniqueId());
        scheduleResync(event.getPlayer().getUniqueId());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() != null && EntityRefreshPolicy.invalidates(
                !event.getFrom().getWorld().equals(event.getTo().getWorld()),
                event.getFrom().getX(), event.getFrom().getY(), event.getFrom().getZ(),
                event.getTo().getX(), event.getTo().getY(), event.getTo().getZ())) {
            invalidateSpawns(event.getPlayer().getUniqueId());
        }
    }
    private void invalidateSpawns(UUID playerId) {
        if (spawnPackets != null) spawnPackets.forget(playerId);
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        resyncTickets.forget(id);
        if (skinCommands != null) skinCommands.forgetPlayer(id);
        if (tabRenderer != null) tabRenderer.forget(id);
        if (spawnPackets != null) spawnPackets.forget(id);

        if (registry != null) registry.clearPlayer(id);
    }

    @Override public void onDisable() {
        resyncTickets.clear();
        if (registry != null) registry.clear();
        if (tabRenderer != null) {
            tabRenderer.restoreApplied();
            tabRenderer.clear();
        }
        tabRenderer = null;
        refreshPlanner = null;
        if (packets != null) packets.removePacketListeners(this);
        packets = null;
        if (spawnPackets != null) spawnPackets.clear();
        spawnPackets = null;
        if (skinCommands != null) skinCommands.clearPending();
        skinCommands = null;
        if (skins != null) skins.close();
        skins = null;
        disguises = null;
        registry = null;
    }

    public DisguiseService getDisguiseService() {
        if (disguises == null) throw new IllegalStateException("Plugin is not enabled");
        assertMainThread();
        return disguises;
    }

    public AsyncSkinService getSkinService() {
        if (skins == null) throw new IllegalStateException("Plugin is not enabled");
        return skins;
    }
    public static final class ResyncTickets {
        private final java.util.Map<UUID, Long> latest = new java.util.HashMap<UUID, Long>();
        private long sequence;

        public long schedule(UUID playerId) {
            long ticket = ++sequence;
            latest.put(java.util.Objects.requireNonNull(playerId, "playerId"), ticket);
            return ticket;
        }

        public boolean consume(UUID playerId, long ticket) {
            Long current = latest.get(java.util.Objects.requireNonNull(playerId, "playerId"));
            if (current == null || current.longValue() != ticket) return false;
            latest.remove(playerId);
            return true;
        }

        public void forget(UUID playerId) { latest.remove(java.util.Objects.requireNonNull(playerId, "playerId")); }
        public int pendingCount() { return latest.size(); }
        public void clear() { latest.clear(); }
    }
}
