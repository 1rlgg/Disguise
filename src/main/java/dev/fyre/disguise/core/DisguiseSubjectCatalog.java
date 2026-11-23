package dev.fyre.disguise.core;

import java.util.Set;
import java.util.UUID;

public interface DisguiseSubjectCatalog {
    Set<UUID> subjectIds();

    default boolean hasAnySubjects() { return !subjectIds().isEmpty(); }

    default boolean hasAnySubjectsFor(UUID viewerId) {
        java.util.Objects.requireNonNull(viewerId, "viewerId");
        return hasAnySubjects();
    }
}
