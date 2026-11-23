package dev.fyre.disguise.render;

import dev.fyre.disguise.render.refresh.RefreshRetryLedger;

import dev.fyre.disguise.render.refresh.RefreshPolicy;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class RefreshAdmissionPolicyTest {
    @Test public void staleQueuedWorkCannotBypassRetryExhaustion() {
        RefreshRetryLedger ledger = new RefreshRetryLedger(1, 4);
        RefreshQueue.Pair pair = new RefreshQueue.Pair(UUID.randomUUID(), UUID.randomUUID());
        assertTrue(ledger.shouldRetry(pair));
        assertFalse(ledger.shouldRetry(pair));
        assertFalse(RefreshPolicy.mayAttempt(ledger.suppressed(pair)));
        ledger.newTransition(pair);
        assertTrue(RefreshPolicy.mayAttempt(ledger.suppressed(pair)));
    }

    @Test public void disconnectForgetsQuarantine() {
        RefreshRetryLedger ledger = new RefreshRetryLedger(0, 4);
        RefreshQueue.Pair pair = new RefreshQueue.Pair(UUID.randomUUID(), UUID.randomUUID());
        assertFalse(ledger.shouldRetry(pair));
        assertFalse(RefreshPolicy.mayAttempt(ledger.suppressed(pair)));
        ledger.forget(pair.getViewer());
        assertTrue(RefreshPolicy.mayAttempt(ledger.suppressed(pair)));
    }
}
