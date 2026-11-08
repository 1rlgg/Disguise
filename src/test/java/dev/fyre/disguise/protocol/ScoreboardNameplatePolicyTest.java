package dev.fyre.disguise.protocol;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScoreboardNameplatePolicyTest {
    @Test public void avoidsReplayingEntitiesControlledByAnotherTeam() {
        assertFalse(ScoreboardNameplatePolicy.mayRefreshEntity(true));
    }

    @Test public void rejectsAliasTeamOwnershipOrExistingOnlinePlayer() {
        assertFalse(ScoreboardNameplatePolicy.mayRefreshEntity(false, true, false));
        assertFalse(ScoreboardNameplatePolicy.mayRefreshEntity(false, false, true));
        assertTrue(ScoreboardNameplatePolicy.mayRefreshEntity(false, false, false));
    }

    @Test public void permitsUnmanagedPlayerNameEntries() {
        assertTrue(ScoreboardNameplatePolicy.mayRefreshEntity(false));
    }
}
