package dev.fyre.disguise.protocol;


public final class NameplateCompatibility {
    private NameplateCompatibility() { }

    public static String reason(boolean sameWorld, boolean visible, boolean teamOwned,
                                boolean spectator, boolean self) {
        return reason(sameWorld, visible, teamOwned, false, false, spectator, self);
    }

    public static String reason(boolean sameWorld, boolean visible, boolean realTeam,
                                boolean aliasTeam, boolean aliasCollidesWithOnlinePlayer,
                                boolean spectator, boolean self) {
        return reason(sameWorld, visible, realTeam, aliasTeam, aliasCollidesWithOnlinePlayer,
                false, spectator, self);
    }

    public static String reason(boolean sameWorld, boolean visible, boolean realTeam,
                                boolean aliasTeam, boolean aliasCollidesWithOnlinePlayer,
                                boolean duplicateViewerAlias, boolean spectator, boolean self) {
        if (self) return "BLOCKED (self-profile)";
        if (!visible) return "BLOCKED (viewer cannot see subject)";
        if (!sameWorld) return "BLOCKED (different worlds)";
        if (spectator) return "BLOCKED (spectator mode)";
        if (realTeam) return "BLOCKED (third-party scoreboard team owns real name)";
        if (aliasTeam) return "BLOCKED (scoreboard team owns alias name)";
        if (aliasCollidesWithOnlinePlayer) return "BLOCKED (alias collides with online player)";
        if (duplicateViewerAlias) return "BLOCKED (another disguised subject uses same alias)";
        return "POTENTIALLY ELIGIBLE (additional tracking and player-state checks still apply)";
    }
}
