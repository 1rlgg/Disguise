package dev.fyre.disguise.api;

import java.util.Optional;
import java.util.UUID;

public interface DisguiseService {
    void setDisguise(Disguise disguise);
    Optional<Disguise> getDisguise(UUID playerId);
    boolean removeDisguise(UUID playerId);

    void setDisguiseFor(UUID viewerId, Disguise disguise);

    Optional<Disguise> getDisguiseFor(UUID viewerId, UUID playerId);

    default Optional<Disguise> getViewerOverride(UUID viewerId, UUID playerId) {
        java.util.Objects.requireNonNull(viewerId, "viewerId");
        java.util.Objects.requireNonNull(playerId, "playerId");
        return Optional.empty();
    }

    boolean removeDisguiseFor(UUID viewerId, UUID playerId);

    void clearPlayer(UUID playerId);

    void clear();
}
