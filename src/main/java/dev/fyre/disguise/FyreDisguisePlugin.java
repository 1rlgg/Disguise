package dev.fyre.disguise;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import dev.fyre.disguise.api.DisguiseService;
import dev.fyre.disguise.command.DisguiseCommand;
import dev.fyre.disguise.core.InMemoryDisguiseService;
import dev.fyre.disguise.core.ObservableDisguiseService;
import dev.fyre.disguise.protocol.PlayerInfoPacketListener;
import dev.fyre.disguise.protocol.PlayerInfoRewriter;
import dev.fyre.disguise.protocol.PlayerInfoTabRefresh;
import dev.fyre.disguise.render.IdentityRenderer;
import dev.fyre.disguise.render.LoggedIdentityRenderer;
import dev.fyre.disguise.render.TabListIdentityRenderer;
import dev.fyre.disguise.skin.AsyncSkinService;
import dev.fyre.disguise.skin.MojangSkinSource;
import dev.fyre.disguise.skin.SkinCache;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Experimental tab-list aliases; entity/skin refresh remains unimplemented. */
public final class FyreDisguisePlugin extends JavaPlugin implements Listener {
    private DisguiseService disguises;
    private AsyncSkinService skins;
    private ProtocolManager packets;

    @Override public void onEnable() {
        saveDefaultConfig();
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        packets = ProtocolLibrary.getProtocolManager();
        boolean experimentalRefresh = getConfig().getBoolean("experimental-tab-refresh", false);
        IdentityRenderer renderer = experimentalRefresh
                ? new TabListIdentityRenderer(new PlayerInfoTabRefresh(packets, registry), getLogger())
                : new LoggedIdentityRenderer(getLogger());
        disguises = new ObservableDisguiseService(registry, renderer, this::onlineViewerIds, this::assertMainThread);
        skins = new AsyncSkinService(new MojangSkinSource(3000, 5000),
                new SkinCache(512, TimeUnit.MINUTES.toNanos(30)), 2, 128);
        getCommand("fyredisguise").setExecutor(new DisguiseCommand(disguises));
        packets.addPacketListener(new PlayerInfoPacketListener(this, new PlayerInfoRewriter(registry)));
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Player-info aliases enabled; experimental tab refresh: " + experimentalRefresh
                + "; world entity refresh NOT implemented.");
    }

    private Collection<UUID> onlineViewerIds() {
        Collection<UUID> ids = new ArrayList<UUID>();
        for (Player player : Bukkit.getOnlinePlayers()) ids.add(player.getUniqueId());
        return ids;
    }

    private void assertMainThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Disguise mutations require the primary server thread");
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        if (disguises != null) disguises.clearPlayer(event.getPlayer().getUniqueId());
    }

    @Override public void onDisable() {
        if (packets != null) packets.removePacketListeners(this);
        packets = null;
        if (skins != null) skins.close();
        skins = null;
        if (disguises != null) disguises.clear();
        disguises = null;
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
