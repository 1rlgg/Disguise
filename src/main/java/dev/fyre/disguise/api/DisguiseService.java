package dev.fyre.disguise.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Describes desired identity state. No client-visible disguise is applied until a packet
 * renderer is installed. Implementations are not guaranteed to be thread safe.
 */
public interface DisguiseService {
    void setDisguise(Disguise disguise);
    Optional<Disguise> getDisguise(UUID playerId);
    boolean removeDisguise(UUID playerId);

    /** Overrides the global identity for a specific viewer. */
    void setDisguiseFor(UUID viewerId, Disguise disguise);

    /** Returns viewer override if present, otherwise the global identity. */
    Optional<Disguise> getDisguiseFor(UUID viewerId, UUID playerId);

    /**
     * Returns an explicit per-viewer override, NEVER a global fallback.
     * Custom implementations can opt in by overriding this method; the
     * conservative default refuses per-viewer skin mutation.
     */
    default Optional<Disguise> getViewerOverride(UUID viewerId, UUID playerId) {
        java.util.Objects.requireNonNull(viewerId, "viewerId");
        java.util.Objects.requireNonNull(playerId, "playerId");
        return Optional.empty();
    }

    /** Removes only a viewer-specific override, revealing the global value if one exists. */
    boolean removeDisguiseFor(UUID viewerId, UUID playerId);

    /** Removes all state where the player is a subject or viewer. */
    void clearPlayer(UUID playerId);

    void clear();
}
