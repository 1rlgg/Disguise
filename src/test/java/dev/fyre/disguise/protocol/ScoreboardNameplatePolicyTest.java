package dev.fyre.disguise.protocol;

import dev.fyre.disguise.protocol.entity.EntityRefreshPolicy;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScoreboardNameplatePolicyTest {
    @Test public void avoidsReplayingEntitiesControlledByAnotherTeam() {
        assertFalse(EntityRefreshPolicy.mayRefreshEntity(true));
    }

    @Test public void rejectsAliasTeamOwnershipOrExistingOnlinePlayer() {
        assertFalse(EntityRefreshPolicy.mayRefreshEntity(false, true, false));
        assertFalse(EntityRefreshPolicy.mayRefreshEntity(false, false, true));
        assertTrue(EntityRefreshPolicy.mayRefreshEntity(false, false, false));
    }

    @Test public void permitsUnmanagedPlayerNameEntries() {
        assertTrue(EntityRefreshPolicy.mayRefreshEntity(false));
    }
}
