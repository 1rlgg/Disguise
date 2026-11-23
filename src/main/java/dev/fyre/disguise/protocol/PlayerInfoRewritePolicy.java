package dev.fyre.disguise.protocol;

import java.util.Objects;
import java.util.UUID;


public final class PlayerInfoRewritePolicy {
    private PlayerInfoRewritePolicy() { }


    public static boolean mayRewrite(UUID viewer, UUID subject) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(subject, "subject");
        return !viewer.equals(subject);
    }
}
