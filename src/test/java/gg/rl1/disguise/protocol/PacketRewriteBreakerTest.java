package gg.rl1.disguise.protocol;

import org.junit.Test;
import static org.junit.Assert.*;

public class PacketRewriteBreakerTest {
    @Test public void permanentlyDisablesAfterRepeatedFailures() {
        PacketRewriteBreaker breaker = new PacketRewriteBreaker(3);
        assertFalse(breaker.isOpen());
        assertFalse(breaker.recordFailure());
        assertFalse(breaker.recordFailure());
        assertTrue(breaker.recordFailure());
        assertTrue(breaker.isOpen());
        assertEquals(3, breaker.failureCount());
        breaker.recordSuccess();
        assertTrue(breaker.isOpen());
    }

    @Test public void successResetsConsecutiveFailures() {
        PacketRewriteBreaker breaker = new PacketRewriteBreaker(2);
        assertFalse(breaker.recordFailure());
        breaker.recordSuccess();
        assertFalse(breaker.recordFailure());
        assertFalse(breaker.isOpen());
        assertTrue(breaker.recordFailure());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidThresholdIsRejected() {
        new PacketRewriteBreaker(0);
    }
}
