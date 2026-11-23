package dev.fyre.disguise.protocol.entity;

import dev.fyre.disguise.api.DisguiseService;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

public final class ViewerAliasCollisionPolicy {
    private ViewerAliasCollisionPolicy() { }

    public static boolean conflicts(DisguiseService disguises, UUID viewer, UUID subject,
                                    String alias, Collection<UUID> onlineSubjects) {
        Objects.requireNonNull(disguises, "disguises");
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(alias, "alias");
        Objects.requireNonNull(onlineSubjects, "onlineSubjects");
        for (UUID other : onlineSubjects) {
            if (other == null || other.equals(subject) || other.equals(viewer)) continue;
            if (disguises.getDisguiseFor(viewer, other)
                    .map(disguise -> disguise.getName().equalsIgnoreCase(alias))
                    .orElse(false)) return true;
        }
        return false;
    }
}
