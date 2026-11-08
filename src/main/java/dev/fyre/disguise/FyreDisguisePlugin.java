package dev.fyre.disguise;

import dev.fyre.disguise.api.DisguiseService;
import dev.fyre.disguise.core.InMemoryDisguiseService;
import dev.fyre.disguise.core.ObservableDisguiseService;
import dev.fyre.disguise.render.LoggedIdentityRenderer;
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

/** Main-thread-only API foundation; client-visible packet rendering is not implemented. */
public final class FyreDisguisePlugin extends JavaPlugin implements Listener {
    private DisguiseService disguises;
    private AsyncSkinService skins;

    @Override public void onEnable() {
        disguises = new ObservableDisguiseService(new InMemoryDisguiseService(),
                new LoggedIdentityRenderer(getLogger()), this::onlineViewerIds);
        skins = new AsyncSkinService(new MojangSkinSource(3000, 5000),
                new SkinCache(512, TimeUnit.MINUTES.toNanos(30)), 2, 128);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("FyreDisguise rendering boundary initialized (visual disguises unavailable)");
    }

    private Collection<UUID> onlineViewerIds() {
        Collection<UUID> ids = new ArrayList<UUID>();
        for (Player player : Bukkit.getOnlinePlayers()) ids.add(player.getUniqueId());
        return ids;
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        if (disguises != null) disguises.clearPlayer(event.getPlayer().getUniqueId());
    }

    @Override public void onDisable() {
        if (skins != null) skins.close();
        skins = null;
        if (disguises != null) disguises.clear();
        disguises = null;
    }

    public DisguiseService getDisguiseService() {
        if (disguises == null) throw new IllegalStateException("Plugin is not enabled");
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Disguise API requires the primary server thread");
        return disguises;
    }

    public AsyncSkinService getSkinService() {
        if (skins == null) throw new IllegalStateException("Plugin is not enabled");
        return skins;
    }
}
