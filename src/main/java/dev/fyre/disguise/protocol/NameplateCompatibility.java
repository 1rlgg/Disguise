package dev.fyre.disguise.protocol;

/**
 * Read-only gate explaining common vanilla/scoreboard reasons an experimental
 * world refresh must not run. No team membership or game rules are changed.
 */
public final class NameplateCompatibility {
    private NameplateCompatibility() { }

    public static String reason(boolean sameWorld, boolean visible, boolean teamOwned,
                                boolean spectator, boolean self) {
        if (self) return "BLOCKED (self-profile)";
        if (!visible) return "BLOCKED (viewer cannot see subject)";
        if (!sameWorld) return "BLOCKED (different worlds)";
        if (spectator) return "BLOCKED (spectator mode)";
        if (teamOwned) return "BLOCKED (third-party scoreboard team owns real name)";
        return "POTENTIALLY ELIGIBLE (additional tracking and player-state checks still apply)";
    }
}
