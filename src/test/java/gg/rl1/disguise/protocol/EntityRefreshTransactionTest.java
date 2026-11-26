package gg.rl1.disguise.protocol;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class EntityRefreshTransactionTest {
    @Test public void recoveryReceivesPreparedCurrentLocationRatherThanOldSnapshot() {
        java.util.List<String> sent = new java.util.ArrayList<String>();
        try {
            EntityRefreshTransaction.execute("destroy", "current-spawn", "current-spawn-recovery",
                    "head", java.util.Arrays.asList("armor"), packet -> {
                        sent.add(packet);
                        if ("current-spawn".equals(packet)) throw new IllegalStateException("packet rejected");
                    });
            fail("Expected failure");
        } catch (IllegalStateException expected) {
            assertEquals(java.util.Arrays.asList("destroy", "current-spawn", "destroy",
                    "current-spawn-recovery", "head", "armor"), sent);
        } catch (InvocationTargetException impossible) {
            fail("Unexpected error");
        }
    }

    @Test public void sendsPreparedPacketsInOrderOnSuccess() throws Exception {
        List<String> sent = new ArrayList<String>();
        EntityRefreshTransaction.execute("destroy", "new-spawn", "original-spawn",
                "head", Arrays.asList("hand", "armor"), sent::add);
        assertEquals(Arrays.asList("destroy", "new-spawn", "head", "hand", "armor"), sent);
    }

    @Test public void equipmentFailureRestoresOriginalSpawnAndAllEquipment() {
        List<String> sent = new ArrayList<String>();
        final boolean[] failOnce = {true};
        try {
            EntityRefreshTransaction.execute("destroy", "new-spawn", "original-spawn",
                    "head", Arrays.asList("hand", "armor"), packet -> {
                        sent.add(packet);
                        if ("hand".equals(packet) && failOnce[0]) {
                            failOnce[0] = false;
                            throw new InvocationTargetException(new Exception("send failed"));
                        }
                    });
            fail("Expected failure");
        } catch (InvocationTargetException expected) {
            assertEquals(Arrays.asList("destroy", "new-spawn", "head", "hand",
                    "destroy", "original-spawn", "head", "hand", "armor"), sent);
        }
    }

    @Test public void spawnFailureAttemptsOriginalSpawnBeforeRethrowing() {
        List<String> sent = new ArrayList<String>();
        try {
            EntityRefreshTransaction.execute("destroy", "new-spawn", "original-spawn",
                    "head", Arrays.asList("hand"), packet -> {
                        sent.add(packet);
                        if ("new-spawn".equals(packet)) throw new IllegalStateException("bad spawn");
                    });
            fail("Expected failure");
        } catch (IllegalStateException expected) {
            assertEquals(Arrays.asList("destroy", "new-spawn", "destroy",
                    "original-spawn", "head", "hand"), sent);
        } catch (InvocationTargetException unexpected) {
            fail("Wrong exception");
        }
    }

    @Test public void recoveryContinuesAfterOriginalSpawnFailure() {
        List<String> sent = new ArrayList<String>();
        try {
            EntityRefreshTransaction.execute("destroy", "new-spawn", "original-spawn",
                    "head", Arrays.asList("hand", "armor"), packet -> {
                        sent.add(packet);
                        if ("new-spawn".equals(packet) || "original-spawn".equals(packet)) {
                            throw new IllegalStateException("cannot spawn");
                        }
                    });
            fail("Expected failure");
        } catch (IllegalStateException expected) {
            assertEquals(1, expected.getSuppressed().length);
            assertEquals(Arrays.asList("destroy", "new-spawn", "destroy", "original-spawn",
                    "head", "hand", "armor"), sent);
        } catch (InvocationTargetException unexpected) {
            fail("Wrong exception");
        }
    }

    @Test public void recoveryFailuresAreSuppressedOnOriginalFailure() {
        try {
            EntityRefreshTransaction.execute("destroy", "new-spawn", "original-spawn",
                    "head", Arrays.asList("hand"), packet -> {
                        if ("new-spawn".equals(packet)) throw new IllegalStateException("first");
                        if ("original-spawn".equals(packet)) throw new IllegalStateException("recovery");
                    });
            fail("Expected failure");
        } catch (IllegalStateException expected) {
            assertEquals("first", expected.getMessage());
            assertEquals(1, expected.getSuppressed().length);
            assertEquals("recovery", expected.getSuppressed()[0].getMessage());
        } catch (InvocationTargetException unexpected) {
            fail("Wrong exception");
        }
    }
}
