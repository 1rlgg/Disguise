package dev.fyre.disguise.protocol;
import org.junit.Test;
import static org.junit.Assert.*;

public class EntityRefreshFailureGateTest {
    @Test public void opensAfterThreeConsecutiveFailures() {
        EntityRefreshFailureGate gate = new EntityRefreshFailureGate(3);
        assertFalse(gate.failure());
        assertFalse(gate.failure());
        gate.success();
        assertEquals(0, gate.failures());
        assertFalse(gate.failure());
        assertFalse(gate.failure());
        assertTrue(gate.failure());
        assertFalse(gate.enabled());
        gate.success();
        assertFalse(gate.enabled());
    }
}
