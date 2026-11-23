package dev.fyre.disguise.core;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.DisguiseService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.UUID;

public final class InMemoryDisguiseService implements DisguiseService, DisguiseSubjectCatalog {
    private final Map<UUID, Disguise> global = new HashMap<UUID, Disguise>();
    private final Map<UUID, Map<UUID, Disguise>> perViewer = new HashMap<UUID, Map<UUID, Disguise>>();
    private Set<UUID> cachedSubjectIds;

    @Override public synchronized void setDisguise(Disguise disguise) {
        Objects.requireNonNull(disguise, "disguise");
        if (global.put(disguise.getPlayerId(), disguise) == null) cachedSubjectIds = null;
    }

    @Override public synchronized Optional<Disguise> getDisguise(UUID playerId) {
        return Optional.ofNullable(global.get(Objects.requireNonNull(playerId, "playerId")));
    }

    @Override public synchronized boolean removeDisguise(UUID playerId) {
        boolean removed = global.remove(Objects.requireNonNull(playerId, "playerId")) != null;
        if (removed) cachedSubjectIds = null;
        return removed;
    }

    @Override public synchronized boolean hasAnySubjects() {
        return !global.isEmpty() || !perViewer.isEmpty();
    }

    @Override public synchronized boolean hasAnySubjectsFor(UUID viewerId) {
        Objects.requireNonNull(viewerId, "viewerId");
        Map<UUID, Disguise> overrides = perViewer.get(viewerId);
        return !global.isEmpty() || (overrides != null && !overrides.isEmpty());
    }

    @Override public synchronized Set<UUID> subjectIds() {
        if (cachedSubjectIds == null) {
            Set<UUID> ids = new HashSet<UUID>(global.keySet());
            for (Map<UUID, Disguise> overrides : perViewer.values()) {
                ids.addAll(overrides.keySet());
            }
            cachedSubjectIds = Collections.unmodifiableSet(ids);
        }
        return cachedSubjectIds;
    }

    @Override public synchronized void clear() {
        global.clear();
        perViewer.clear();
        cachedSubjectIds = null;
    }

    @Override public synchronized void setDisguiseFor(UUID viewerId, Disguise disguise) {
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(disguise, "disguise");
        Map<UUID, Disguise> overrides = perViewer.get(viewerId);
        if (overrides == null) {
            overrides = new HashMap<UUID, Disguise>();
            perViewer.put(viewerId, overrides);
        }
        if (overrides.put(disguise.getPlayerId(), disguise) == null) cachedSubjectIds = null;
    }

    @Override public synchronized Optional<Disguise> getDisguiseFor(UUID viewerId, UUID playerId) {
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(playerId, "playerId");
        Map<UUID, Disguise> overrides = perViewer.get(viewerId);
        Disguise override = overrides == null ? null : overrides.get(playerId);
        if (override != null) return Optional.of(override);
        return Optional.ofNullable(global.get(playerId));
    }

    @Override public synchronized Optional<Disguise> getViewerOverride(UUID viewerId, UUID playerId) {
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(playerId, "playerId");
        Map<UUID, Disguise> overrides = perViewer.get(viewerId);
        return Optional.ofNullable(overrides == null ? null : overrides.get(playerId));
    }

    @Override public synchronized boolean removeDisguiseFor(UUID viewerId, UUID playerId) {
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(playerId, "playerId");
        Map<UUID, Disguise> overrides = perViewer.get(viewerId);
        if (overrides == null) return false;
        boolean removed = overrides.remove(playerId) != null;
        if (overrides.isEmpty()) perViewer.remove(viewerId);
        if (removed) cachedSubjectIds = null;
        return removed;
    }

    @Override public synchronized void clearPlayer(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        global.remove(playerId);
        perViewer.remove(playerId);
        cachedSubjectIds = null;
        perViewer.values().removeIf(overrides -> {
            overrides.remove(playerId);
            return overrides.isEmpty();
        });
    }
}
