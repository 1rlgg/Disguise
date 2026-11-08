package dev.fyre.disguise.protocol;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.DisguiseService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only per-viewer identity lookup for a future tracked player spawn.
 * Does not send or alter entity packets, UUIDs, hitboxes or visibility.
 */
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
