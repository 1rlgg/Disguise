package dev.fyre.disguise.render;

import dev.fyre.disguise.api.Disguise;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Desired transition for one subject as observed by one viewer. */
public final class IdentityTransition {
    private final UUID viewerId;
    private final UUID subjectId;
    private final Disguise before;
    private final Disguise after;

    public IdentityTransition(UUID viewerId, UUID subjectId, Disguise before, Disguise after) {
        this.viewerId = Objects.requireNonNull(viewerId, "viewerId");
        this.subjectId = Objects.requireNonNull(subjectId, "subjectId");
        if (before != null && !subjectId.equals(before.getPlayerId())) throw new IllegalArgumentException("Before subject mismatch");
        if (after != null && !subjectId.equals(after.getPlayerId())) throw new IllegalArgumentException("After subject mismatch");
        this.before = before;
        this.after = after;
    }

    public UUID getViewerId() { return viewerId; }
    public UUID getSubjectId() { return subjectId; }
    public Optional<Disguise> getBefore() { return Optional.ofNullable(before); }
    public Optional<Disguise> getAfter() { return Optional.ofNullable(after); }
}
