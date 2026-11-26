package gg.rl1.disguise.protocol;
import org.junit.Test;
import static org.junit.Assert.*;

public class EntityRefreshFailureGateTest {
    @Test public void opensAfterThreeConsecutiveFailures() {
        PacketRewriteBreaker gate = new PacketRewriteBreaker(3);
        assertFalse(gate.recordFailure());
        assertFalse(gate.recordFailure());
        gate.recordSuccess();
        assertEquals(0, gate.failureCount());
        assertFalse(gate.recordFailure());
        assertFalse(gate.recordFailure());
        assertTrue(gate.recordFailure());
        assertFalse(!gate.isOpen());
        gate.recordSuccess();
        assertFalse(!gate.isOpen());
    }
}
