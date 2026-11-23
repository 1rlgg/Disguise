package dev.fyre.disguise.protocol.entity;

import dev.fyre.disguise.api.DisguiseService;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

public final class EntityRefreshPolicy {
    private EntityRefreshPolicy() { }

    public static boolean permitted(boolean sameWorld, boolean visible, boolean online,
                                    boolean dead, boolean sleeping, boolean mounted,
                                    boolean inTrackingRange, boolean hasMatchingSpawn) {
        return permitted(sameWorld, visible, online, dead, sleeping, mounted,
                false, inTrackingRange, hasMatchingSpawn);
    }

    public static boolean permitted(boolean sameWorld, boolean visible, boolean online,
                                    boolean dead, boolean sleeping, boolean mounted,
                                    boolean spectator, boolean inTrackingRange,
                                    boolean hasMatchingSpawn) {
        return sameWorld && visible && online && !dead && !sleeping && !mounted
                && !spectator && inTrackingRange && hasMatchingSpawn;
    }

    public static boolean withinTrackingRange(double distanceSquared, int viewDistanceChunks) {
        if (Double.isNaN(distanceSquared) || Double.isInfinite(distanceSquared) || distanceSquared < 0
                || viewDistanceChunks < 1) return false;
        double limit = Math.min(viewDistanceChunks * 16.0D, 48.0D);
        return distanceSquared <= limit * limit;
    }

    public static boolean mayRefreshEntity(boolean scoreboardTeamOwnsRealName) {
        return mayRefreshEntity(scoreboardTeamOwnsRealName, false, false);
    }

    public static boolean mayRefreshEntity(boolean realNameTeam, boolean aliasNameTeam,
                                           boolean aliasConflictsWithOnlinePlayer) {
        return !realNameTeam && !aliasNameTeam && !aliasConflictsWithOnlinePlayer;
    }
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
    public static boolean invalidates(boolean worldChanged,
                                      double fromX, double fromY, double fromZ,
                                      double toX, double toY, double toZ) {
        return worldChanged || Double.compare(fromX, toX) != 0
                || Double.compare(fromY, toY) != 0
                || Double.compare(fromZ, toZ) != 0;
    }
    public static String nameplateReason(boolean sameWorld, boolean visible, boolean teamOwned,
                                boolean spectator, boolean self) {
        return nameplateReason(sameWorld, visible, teamOwned, false, false, spectator, self);
    }

    public static String nameplateReason(boolean sameWorld, boolean visible, boolean realTeam,
                                boolean aliasTeam, boolean aliasCollidesWithOnlinePlayer,
                                boolean spectator, boolean self) {
        return nameplateReason(sameWorld, visible, realTeam, aliasTeam, aliasCollidesWithOnlinePlayer,
                false, spectator, self);
    }

    public static String nameplateReason(boolean sameWorld, boolean visible, boolean realTeam,
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
