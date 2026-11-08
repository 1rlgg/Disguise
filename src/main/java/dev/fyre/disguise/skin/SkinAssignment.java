package dev.fyre.disguise.skin;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.DisguiseService;
import dev.fyre.disguise.api.SkinProperty;
import java.util.Objects;
import java.util.UUID;

/**
 * Applies signed texture values to immutable disguise requests. Must run on the
 * Bukkit primary thread; a stale asynchronous response is rejected by identity.
 */
public final class SkinAssignment {
    private final DisguiseService disguises;

    public SkinAssignment(DisguiseService disguises) {
        this.disguises = Objects.requireNonNull(disguises, "disguises");
    }

    private void validate(UUID playerId, Disguise expected, SkinProperty skin) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(skin, "skin");
        if (!skin.isSigned()) throw new IllegalArgumentException("Skin must include a signature");
        if (!playerId.equals(expected.getPlayerId())) throw new IllegalArgumentException("Target mismatch");
    }

    public boolean applyIfCurrent(UUID playerId, Disguise expected, SkinProperty skin) {
        validate(playerId, expected, skin);
        if (disguises.getDisguise(playerId).orElse(null) != expected) return false;
        disguises.setDisguise(new Disguise(playerId, expected.getName(), skin));
        return true;
    }

    /**
     * A viewer-specific override must still be present; a global disguise
     * fallback is not eligible for per-viewer mutation.
     */
    public boolean applyForViewerIfCurrent(UUID viewerId, UUID playerId,
                                           Disguise expected, SkinProperty skin) {
        Objects.requireNonNull(viewerId, "viewerId");
        validate(playerId, expected, skin);
        if (disguises.getDisguiseFor(viewerId, playerId).orElse(null) != expected) return false;
        // A fallback to the global request must never become a new viewer override.
        if (disguises.getDisguise(playerId).orElse(null) == expected) return false;
        disguises.setDisguiseFor(viewerId, new Disguise(playerId, expected.getName(), skin));
        return true;
    }
}
