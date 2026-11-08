package dev.fyre.disguise;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.DisguiseService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Foundation plugin. Disguise state is memory-only and does not yet alter player appearance.
 * Bukkit-facing callers must use the primary server thread.
 */
public final class FyreDisguisePlugin extends JavaPlugin {
    private DisguiseService disguises;

    @Override public void onEnable() {
        disguises = new MemoryDisguiseService();
        getLogger().info("FyreDisguise API initialized (packet disguises not yet implemented)");
    }

    @Override public void onDisable() {
        if (disguises != null) disguises.clear();
        disguises = null;
    }

    public DisguiseService getDisguiseService() {
        if (disguises == null) throw new IllegalStateException("Plugin is not enabled");
        return disguises;
    }

    private static final class MemoryDisguiseService implements DisguiseService {
        private final Map<UUID, Disguise> disguises = new HashMap<UUID, Disguise>();

        @Override public void setDisguise(Disguise disguise) {
            Objects.requireNonNull(disguise, "disguise");
            disguises.put(disguise.getPlayerId(), disguise);
        }

        @Override public Optional<Disguise> getDisguise(UUID playerId) {
            return Optional.ofNullable(disguises.get(Objects.requireNonNull(playerId, "playerId")));
        }

        @Override public boolean removeDisguise(UUID playerId) {
            return disguises.remove(Objects.requireNonNull(playerId, "playerId")) != null;
        }

        @Override public void clear() { disguises.clear(); }
    }
}
