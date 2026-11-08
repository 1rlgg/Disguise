package dev.fyre.disguise.core;

import java.util.Set;
import java.util.UUID;

/**
 * A snapshot of player IDs that currently have a global disguise or at least
 * one viewer-specific override. Used to publish restoration transitions.
 */
public interface DisguiseSubjectCatalog {
    Set<UUID> subjectIds();

    /** Cheap hot-path test for outgoing player-info packets. */
    default boolean hasAnySubjects() { return !subjectIds().isEmpty(); }
}
