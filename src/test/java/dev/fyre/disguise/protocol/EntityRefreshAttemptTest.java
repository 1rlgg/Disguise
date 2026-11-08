package dev.fyre.disguise.protocol;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class EntityRefreshAttemptTest {
    @Test public void skippedWorldRefreshAndBrokenTabDoNotCountEntityFailure() {
        EntityRefreshFailureGate gate = new EntityRefreshFailureGate(2);
        EntityRefreshAttempt attempt = new EntityRefreshAttempt(gate);
        try {
            attempt.deliver(() -> false, () -> { throw new IllegalStateException("tab down"); });
            fail("Expected broken tab");
        } catch (IllegalStateException expected) {
            assertEquals(0, gate.failures());
            assertTrue(gate.enabled());
        } catch (InvocationTargetException unexpected) {
            fail("Wrong failure type");
        }
    }

    @Test public void realEntityFailuresOpenGateAndSubsequentCallsUseTabOnly() {
        EntityRefreshFailureGate gate = new EntityRefreshFailureGate(2);
        EntityRefreshAttempt attempt = new EntityRefreshAttempt(gate);
        AtomicInteger world = new AtomicInteger(), tab = new AtomicInteger();
        for (int i = 0; i < 2; i++) {
            try {
                attempt.deliver(() -> {
                    world.incrementAndGet();
                    throw new InvocationTargetException(new Exception("world down"));
                }, tab::incrementAndGet);
                fail("Expected failure");
            } catch (InvocationTargetException expected) {
                if (i == 1) assertEquals(1, expected.getSuppressed().length);
            }
            assertEquals(i + 1, tab.get());
        }
        assertFalse(gate.enabled());
        try {
            attempt.deliver(() -> { world.incrementAndGet(); return true; }, tab::incrementAndGet);
        } catch (InvocationTargetException unexpected) { fail("No error expected"); }
        assertEquals(2, world.get());
        assertEquals(3, tab.get());
    }

    @Test public void firstWorldFailureAttemptsTabRecoveryWithoutOpeningCircuit() {
        EntityRefreshFailureGate gate = new EntityRefreshFailureGate(3);
        EntityRefreshAttempt attempt = new EntityRefreshAttempt(gate);
        AtomicInteger tab = new AtomicInteger();
        try {
            attempt.deliver(() -> { throw new IllegalStateException("world unavailable"); },
                    tab::incrementAndGet);
            fail("Expected world transport failure");
        } catch (IllegalStateException expected) {
            assertEquals(0, expected.getSuppressed().length);
            assertEquals(1, tab.get());
            assertTrue(gate.enabled());
            assertEquals(1, gate.failures());
        } catch (InvocationTargetException unexpected) {
            fail("Unexpected exception");
        }
    }

    @Test public void failedImmediateFallbackDoesNotHideOriginalWorldFailure() {
        EntityRefreshFailureGate gate = new EntityRefreshFailureGate(1);
        EntityRefreshAttempt attempt = new EntityRefreshAttempt(gate);
        try {
            attempt.deliver(() -> { throw new InvocationTargetException(new Exception("world")); },
                    () -> { throw new IllegalStateException("tab failed"); });
            fail("Expected world failure");
        } catch (InvocationTargetException expected) {
            assertEquals(2, expected.getSuppressed().length);
            assertTrue(expected.getSuppressed()[0]
                    instanceof EntityRefreshAttempt.EntityCircuitOpenedException);
            assertEquals("tab failed", expected.getSuppressed()[1].getMessage());
            assertFalse(gate.enabled());
        }
    }

    @Test public void successfulEntityUpdateResetsActualWorldFailureCount() throws Exception {
        EntityRefreshFailureGate gate = new EntityRefreshFailureGate(2);
        EntityRefreshAttempt attempt = new EntityRefreshAttempt(gate);
        try {
            attempt.deliver(() -> { throw new IllegalStateException("bad world"); }, () -> { });
            fail("Expected failure");
        } catch (IllegalStateException expected) { assertEquals(1, gate.failures()); }
        attempt.deliver(() -> true, () -> fail("Unneeded tab refresh"));
        assertEquals(0, gate.failures());
    }
}
