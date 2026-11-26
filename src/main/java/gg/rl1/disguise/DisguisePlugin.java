package gg.rl1.disguise;

import gg.rl1.disguise.api.Disguise;
import gg.rl1.disguise.command.DisguiseDiagnostics;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import gg.rl1.disguise.api.DisguiseService;
import gg.rl1.disguise.command.DisguiseCommand;
import gg.rl1.disguise.command.SkinCommandHandler;
import gg.rl1.disguise.core.InMemoryDisguiseService;
import gg.rl1.disguise.core.ObservableDisguiseService;
import gg.rl1.disguise.protocol.LegacyEntityRefresh;
import gg.rl1.disguise.protocol.packet.PlayerInfoPacketListener;
import gg.rl1.disguise.protocol.packet.PlayerInfoRewriter;
import gg.rl1.disguise.protocol.packet.PlayerInfoTabRefresh;
import gg.rl1.disguise.protocol.spawn.SpawnPacketObserver;
import gg.rl1.disguise.protocol.spawn.TrackedSpawnPackets;
import java.util.function.Consumer;
import gg.rl1.disguise.render.identity.IdentityTransition;
import gg.rl1.disguise.render.RefreshQueue;
import gg.rl1.disguise.render.RefreshTargetPlanner;
import gg.rl1.disguise.render.TabListIdentityRenderer;
import gg.rl1.disguise.skin.AsyncSkinService;
import gg.rl1.disguise.skin.MojangSkinSource;
import gg.rl1.disguise.skin.cache.SkinCache;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import gg.rl1.disguise.protocol.entity.EntityRefreshPolicy;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class DisguisePlugin extends JavaPlugin implements Listener {
    private DisguiseService disguises;
    private InMemoryDisguiseService registry;
    private AsyncSkinService skins;
    private SkinCommandHandler skinCommands;
    private ProtocolManager packets;
    private TabListIdentityRenderer tabRenderer;
    private RefreshTargetPlanner refreshPlanner;
    private TrackedSpawnPackets spawnPackets;
    private final Map<UUID, BukkitTask> resyncTasks = new HashMap<UUID, BukkitTask>();
    private final Set<UUID> onlineViewers = new LinkedHashSet<UUID>();

    @Override public void onEnable() {
        saveDefaultConfig();
        registry = new InMemoryDisguiseService();
        onlineViewers.clear();
        for (Player player : Bukkit.getOnlinePlayers()) onlineViewers.add(player.getUniqueId());
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
        Consumer<IdentityTransition> renderer = experimentalRefresh ? tabRenderer : transition -> getLogger().fine("Identity transition for viewer "
                + transition.getViewerId() + " subject " + transition.getSubjectId()
                + ": " + (transition.getAfter().isPresent() ? "disguise requested" : "restore requested"));
        disguises = new ObservableDisguiseService(registry, renderer, this::onlineViewerIds, this::assertMainThread);
        skins = new AsyncSkinService(new MojangSkinSource(3000, 5000),
                new SkinCache(512, TimeUnit.MINUTES.toNanos(30)), 2, 128);
        skinCommands = new SkinCommandHandler(this, disguises, skins);
        getCommand("disguise").setExecutor(new DisguiseCommand(disguises,
                skinCommands, () -> new gg.rl1.disguise.command.DisguiseDiagnostics(
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
        return onlineViewers;
    }

    private void assertMainThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Disguise mutations require the primary server thread");
        if (!isEnabled() || disguises == null) {
            throw new IllegalStateException("Disguise mutations require an enabled plugin");
        }
    }

    private void scheduleResync(UUID playerId) {
        if (tabRenderer == null) return;
        BukkitTask previous = resyncTasks.remove(playerId);
        if (previous != null) previous.cancel();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(this, () -> {
            resyncTasks.remove(playerId);
            if (tabRenderer == null || refreshPlanner == null || !isEnabled()) return;
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) return;
            for (RefreshQueue.Pair pair : refreshPlanner.around(playerId, onlineViewerIds(),
                    candidate -> tabRenderer.wasApplied(candidate.getViewer(), candidate.getSubject()))) {
                tabRenderer.enqueue(pair.getViewer(), pair.getSubject());
            }
        }, 2L);
        resyncTasks.put(playerId, task);
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        onlineViewers.add(id);
        scheduleResync(id);
    }
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
        onlineViewers.remove(id);
        BukkitTask pendingResync = resyncTasks.remove(id);
        if (pendingResync != null) pendingResync.cancel();
        if (skinCommands != null) skinCommands.forgetPlayer(id);
        if (tabRenderer != null) tabRenderer.forget(id);
        if (spawnPackets != null) spawnPackets.forget(id);

        if (registry != null) registry.clearPlayer(id);
    }

    @Override public void onDisable() {
        for (BukkitTask task : resyncTasks.values()) task.cancel();
        resyncTasks.clear();
        onlineViewers.clear();
        if (skinCommands != null) skinCommands.clearPending();
        skinCommands = null;
        if (skins != null) skins.close();
        skins = null;
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
}
