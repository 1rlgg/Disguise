package dev.fyre.disguise.render;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.SkinProperty;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class IdentityPlanner {
    public Optional<IdentityTransition> plan(UUID viewer, UUID subject, Disguise before, Disguise after) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(subject, "subject");
        if (same(before, after)) return Optional.empty();
        return Optional.of(new IdentityTransition(viewer, subject, before, after));
    }

    private boolean same(Disguise left, Disguise right) {
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
