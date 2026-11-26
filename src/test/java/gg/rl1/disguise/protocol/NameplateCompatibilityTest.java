package gg.rl1.disguise.protocol;

import gg.rl1.disguise.protocol.entity.EntityRefreshPolicy;

import org.junit.Test;
import static org.junit.Assert.*;

public class NameplateCompatibilityTest {
    @Test public void neverOverridesVanishOrScoreboardOwnership() {
        assertTrue(EntityRefreshPolicy.nameplateReason(true, false, false, false, false)
                .contains("cannot see"));
        assertTrue(EntityRefreshPolicy.nameplateReason(true, true, true, false, false)
                .contains("scoreboard"));
        assertTrue(EntityRefreshPolicy.nameplateReason(true, true, false, false, true)
                .contains("self"));
    }

    @Test public void spectatorAndWorldMismatchAreReportedWithoutPacketMutation() {
        assertTrue(EntityRefreshPolicy.nameplateReason(false, true, false, false, false)
                .contains("different worlds"));
        assertTrue(EntityRefreshPolicy.nameplateReason(true, true, false, true, false)
                .contains("spectator"));
    }

    @Test public void aliasTeamAndOnlineProfileConflictsAreVisibleToAdmin() {
        assertTrue(EntityRefreshPolicy.nameplateReason(true, true, false, true, false, false, false)
                .contains("alias name"));
        assertTrue(EntityRefreshPolicy.nameplateReason(true, true, false, false, true, false, false)
                .contains("online player"));
    }

    @Test public void reportsDuplicateViewerAliasesWithoutChangingScoreboards() {
        assertTrue(EntityRefreshPolicy.nameplateReason(true, true, false, false, false, true, false, false)
                .contains("another disguised subject"));
    }

    @Test public void greenLightIsOnlyPreliminary() {
        assertTrue(EntityRefreshPolicy.nameplateReason(true, true, false, false, false)
                .contains("POTENTIALLY ELIGIBLE"));
    }
}
