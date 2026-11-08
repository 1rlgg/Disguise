package dev.fyre.disguise.protocol;

/**
 * Fail-safe decision for a player whose viewer-specific scoreboard already
 * owns a real-name team entry. Team packets can carry prefixes, invisibility
 * and nametag rules that an entity-only refresh cannot safely reproduce.
 */
public final class ScoreboardNameplatePolicy {
    private ScoreboardNameplatePolicy() { }

    /**
     * World entity refresh is allowed only when we are not conflicting with
     * an existing scoreboard team. The tab-list path remains independent.
     */
    public static boolean mayRefreshEntity(boolean scoreboardTeamOwnsRealName) {
        return mayRefreshEntity(scoreboardTeamOwnsRealName, false, false);
    }

    /**
     * Legacy clients may resolve a renamed spawn against scoreboard entries
     * and existing online profiles. Do not forcibly replace any of those.
     */
    public static boolean mayRefreshEntity(boolean realNameTeam, boolean aliasNameTeam,
                                           boolean aliasConflictsWithOnlinePlayer) {
        return !realNameTeam && !aliasNameTeam && !aliasConflictsWithOnlinePlayer;
    }
}
