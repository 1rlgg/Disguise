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

    @Test public void concurrentSuccessCannotReopenTrippedCircuit() throws Exception {
        for (int round = 0; round < 50; round++) {
            PacketRewriteBreaker breaker = new PacketRewriteBreaker(1);
            java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.FutureTask<Void> success = new java.util.concurrent.FutureTask<>(() -> {
                start.await();
                for (int i = 0; i < 200; i++) breaker.recordSuccess();
                return null;
            });
            Thread worker = new Thread(success);
            worker.start();
            start.countDown();
            breaker.recordFailure();
            success.get(5, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(breaker.isOpen());
            assertEquals(1, breaker.failureCount());
        }
    }
}
