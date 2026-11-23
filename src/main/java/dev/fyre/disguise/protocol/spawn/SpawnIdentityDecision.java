package dev.fyre.disguise.protocol.spawn;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.DisguiseService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class SpawnIdentityDecision {
    private final DisguiseService disguises;

    public SpawnIdentityDecision(DisguiseService disguises) {
        this.disguises = Objects.requireNonNull(disguises, "disguises");
    }

    public Optional<Disguise> desiredIdentity(UUID viewer, UUID subject, boolean visible) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(subject, "subject");
        if (!visible || viewer.equals(subject)) return Optional.empty();
        return disguises.getDisguiseFor(viewer, subject);
    }
}
