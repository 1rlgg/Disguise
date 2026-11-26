package gg.rl1.disguise.skin;

import gg.rl1.disguise.api.Disguise;
import gg.rl1.disguise.api.DisguiseService;
import gg.rl1.disguise.api.SkinProperty;
import java.util.Objects;
import java.util.UUID;

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

    public boolean applyForViewerIfCurrent(UUID viewerId, UUID playerId,
                                           Disguise expected, SkinProperty skin) {
        Objects.requireNonNull(viewerId, "viewerId");
        validate(playerId, expected, skin);

        if (disguises.getViewerOverride(viewerId, playerId).orElse(null) != expected) return false;
        disguises.setDisguiseFor(viewerId, new Disguise(playerId, expected.getName(), skin));
        return true;
    }
}
