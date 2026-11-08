package dev.fyre.disguise.protocol;

import org.junit.Test;
import static org.junit.Assert.*;

public class NameplateCompatibilityTest {
    @Test public void neverOverridesVanishOrScoreboardOwnership() {
        assertTrue(NameplateCompatibility.reason(true, false, false, false, false)
                .contains("cannot see"));
        assertTrue(NameplateCompatibility.reason(true, true, true, false, false)
                .contains("scoreboard"));
        assertTrue(NameplateCompatibility.reason(true, true, false, false, true)
                .contains("self"));
    }

    @Test public void spectatorAndWorldMismatchAreReportedWithoutPacketMutation() {
        assertTrue(NameplateCompatibility.reason(false, true, false, false, false)
                .contains("different worlds"));
        assertTrue(NameplateCompatibility.reason(true, true, false, true, false)
                .contains("spectator"));
    }

    @Test public void aliasTeamAndOnlineProfileConflictsAreVisibleToAdmin() {
        assertTrue(NameplateCompatibility.reason(true, true, false, true, false, false, false)
                .contains("alias name"));
        assertTrue(NameplateCompatibility.reason(true, true, false, false, true, false, false)
                .contains("online player"));
    }

    @Test public void reportsDuplicateViewerAliasesWithoutChangingScoreboards() {
        assertTrue(NameplateCompatibility.reason(true, true, false, false, false, true, false, false)
                .contains("another disguised subject"));
    }

    @Test public void greenLightIsOnlyPreliminary() {
        assertTrue(NameplateCompatibility.reason(true, true, false, false, false)
                .contains("POTENTIALLY ELIGIBLE"));
    }
}
