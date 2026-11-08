package dev.fyre.disguise.protocol;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScoreboardNameplatePolicyTest {
    @Test public void avoidsReplayingEntitiesControlledByAnotherTeam() {
        assertFalse(ScoreboardNameplatePolicy.mayRefreshEntity(true));
    }

    @Test public void permitsUnmanagedPlayerNameEntries() {
        assertTrue(ScoreboardNameplatePolicy.mayRefreshEntity(false));
    }
}
