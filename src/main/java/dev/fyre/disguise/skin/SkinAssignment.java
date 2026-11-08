package dev.fyre.disguise.skin;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.DisguiseService;
import dev.fyre.disguise.api.SkinProperty;
import java.util.Objects;
import java.util.UUID;

/**
 * Applies a fetched skin to an existing global alias only if its immutable
 * identity request is still current. Invoke only on the server main thread.
 */
public final class SkinAssignment {
    private final DisguiseService disguises;

    public SkinAssignment(DisguiseService disguises) {
        this.disguises = Objects.requireNonNull(disguises, "disguises");
    }

    /**
     * Returns false when the disguise changed or was removed while I/O ran.
     * Object identity intentionally prevents stale responses overwriting newer requests.
     */
    public boolean applyIfCurrent(UUID playerId, Disguise expected, SkinProperty skin) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(skin, "skin");
        if (!skin.isSigned()) throw new IllegalArgumentException("Skin must include a signature");
        if (!playerId.equals(expected.getPlayerId())) throw new IllegalArgumentException("Target mismatch");
        if (disguises.getDisguise(playerId).orElse(null) != expected) return false;
        disguises.setDisguise(new Disguise(playerId, expected.getName(), skin));
        return true;
    }
}
