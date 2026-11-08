package dev.fyre.disguise.core;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.DisguiseService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Thread-safe desired identity registry, including viewer-specific overrides.
 * Writes should originate on the Bukkit primary thread. Reads are safe for packet listeners.
 */
public final class InMemoryDisguiseService implements DisguiseService {
    private final Map<UUID, Disguise> global = new HashMap<UUID, Disguise>();
    private final Map<UUID, Map<UUID, Disguise>> perViewer = new HashMap<UUID, Map<UUID, Disguise>>();

    @Override public synchronized void setDisguise(Disguise disguise) {
        Objects.requireNonNull(disguise, "disguise");
        global.put(disguise.getPlayerId(), disguise);
    }

    @Override public synchronized Optional<Disguise> getDisguise(UUID playerId) {
        return Optional.ofNullable(global.get(Objects.requireNonNull(playerId, "playerId")));
    }

    @Override public synchronized boolean removeDisguise(UUID playerId) {
        return global.remove(Objects.requireNonNull(playerId, "playerId")) != null;
    }

    @Override public synchronized void clear() {
        global.clear();
        perViewer.clear();
    }

    @Override public synchronized void setDisguiseFor(UUID viewerId, Disguise disguise) {
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(disguise, "disguise");
        Map<UUID, Disguise> overrides = perViewer.get(viewerId);
        if (overrides == null) {
            overrides = new HashMap<UUID, Disguise>();
            perViewer.put(viewerId, overrides);
        }
        overrides.put(disguise.getPlayerId(), disguise);
    }

    @Override public synchronized Optional<Disguise> getDisguiseFor(UUID viewerId, UUID playerId) {
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(playerId, "playerId");
        Map<UUID, Disguise> overrides = perViewer.get(viewerId);
        if (overrides != null && overrides.containsKey(playerId)) {
            return Optional.of(overrides.get(playerId));
        }
        return getDisguise(playerId);
    }

    @Override public synchronized boolean removeDisguiseFor(UUID viewerId, UUID playerId) {
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(playerId, "playerId");
        Map<UUID, Disguise> overrides = perViewer.get(viewerId);
        if (overrides == null) return false;
        boolean removed = overrides.remove(playerId) != null;
        if (overrides.isEmpty()) perViewer.remove(viewerId);
        return removed;
    }

    @Override public synchronized void clearPlayer(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        global.remove(playerId);
        perViewer.remove(playerId);
        perViewer.values().removeIf(overrides -> {
            overrides.remove(playerId);
            return overrides.isEmpty();
        });
    }
}
