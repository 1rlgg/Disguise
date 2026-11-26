package gg.rl1.disguise.render.identity;

import gg.rl1.disguise.api.Disguise;
import gg.rl1.disguise.api.SkinProperty;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
    public static Optional<IdentityTransition> plan(UUID viewer, UUID subject, Disguise before, Disguise after) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(subject, "subject");
        if (same(before, after)) return Optional.empty();
        return Optional.of(new IdentityTransition(viewer, subject, before, after));
    }

    private static boolean same(Disguise left, Disguise right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        if (!left.getPlayerId().equals(right.getPlayerId()) || !left.getName().equals(right.getName())) return false;
        Optional<SkinProperty> a = left.getSkin();
        Optional<SkinProperty> b = right.getSkin();
        if (!a.isPresent() || !b.isPresent()) return a.isPresent() == b.isPresent();
        return a.get().getValue().equals(b.get().getValue())
                && Objects.equals(a.get().getSignature(), b.get().getSignature());
    }
}
