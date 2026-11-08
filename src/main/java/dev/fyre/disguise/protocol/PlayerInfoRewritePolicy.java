package dev.fyre.disguise.protocol;

import java.util.Objects;
import java.util.UUID;

/** Pure admission check for a recipient-specific player-info profile rewrite. */
public final class PlayerInfoRewritePolicy {
    private PlayerInfoRewritePolicy() { }

    /**
     * The subject's own profile must remain unchanged; only other viewers
     * receive per-viewer aliases. Callers still must check that state exists.
     */
    public static boolean mayRewrite(UUID viewer, UUID subject) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(subject, "subject");
        return !viewer.equals(subject);
    }
}
