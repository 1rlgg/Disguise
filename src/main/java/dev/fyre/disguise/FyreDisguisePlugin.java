package dev.fyre.disguise;

import dev.fyre.disguise.api.DisguiseService;
import dev.fyre.disguise.core.InMemoryDisguiseService;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Foundation plugin. State registration is not a packet disguise.
 */
public final class FyreDisguisePlugin extends JavaPlugin implements Listener {
    private DisguiseService disguises;

    @Override public void onEnable() {
        disguises = new InMemoryDisguiseService();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("FyreDisguise state API enabled; packet rendering is not implemented");
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        if (disguises != null) disguises.clearPlayer(event.getPlayer().getUniqueId());
    }

    @Override public void onDisable() {
        if (disguises != null) disguises.clear();
        disguises = null;
    }

    public DisguiseService getDisguiseService() {
        if (disguises == null) throw new IllegalStateException("Plugin is not enabled");
        return disguises;
    }
}
