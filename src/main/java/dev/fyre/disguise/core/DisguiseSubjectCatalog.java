package dev.fyre.disguise.core;

import java.util.Set;
import java.util.UUID;

public interface DisguiseSubjectCatalog {
    Set<UUID> subjectIds();

    default boolean hasAnySubjects() { return !subjectIds().isEmpty(); }
}
