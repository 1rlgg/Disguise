package dev.fyre.disguise.protocol.entity;

public final class ScoreboardNameplatePolicy {
    private ScoreboardNameplatePolicy() { }

    public static boolean mayRefreshEntity(boolean scoreboardTeamOwnsRealName) {
        return mayRefreshEntity(scoreboardTeamOwnsRealName, false, false);
    }

    public static boolean mayRefreshEntity(boolean realNameTeam, boolean aliasNameTeam,
                                           boolean aliasConflictsWithOnlinePlayer) {
        return !realNameTeam && !aliasNameTeam && !aliasConflictsWithOnlinePlayer;
    }
}
