package gg.rl1.disguise.render;

import gg.rl1.disguise.render.refresh.RefreshRetryLedger;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class RefreshRetryLedgerTest {
    private RefreshQueue.Pair pair() {
        return new RefreshQueue.Pair(UUID.randomUUID(), UUID.randomUUID());
    }

    @Test public void retriesExactlyTwiceAndDropsTrackedFailureState() {
        RefreshRetryLedger ledger = new RefreshRetryLedger(2, 10);
        RefreshQueue.Pair pair = pair();
        assertTrue(ledger.shouldRetry(pair));
        assertTrue(ledger.shouldRetry(pair));
        assertFalse(ledger.shouldRetry(pair));
        assertEquals(0, ledger.size());
        assertEquals(1, ledger.exhaustedCount());
        assertTrue(ledger.suppressed(pair));
        assertFalse(ledger.shouldRetry(pair));
        ledger.newTransition(pair);
        assertFalse(ledger.suppressed(pair));
        assertTrue(ledger.shouldRetry(pair));
    }

    @Test public void successResetsFailureAttempts() {
        RefreshRetryLedger ledger = new RefreshRetryLedger(1, 10);
        RefreshQueue.Pair pair = pair();
        assertTrue(ledger.shouldRetry(pair));
        ledger.succeeded(pair);
        assertTrue(ledger.shouldRetry(pair));
    }

    @Test public void forgetClearsBothViewerAndSubject() {
        RefreshRetryLedger ledger = new RefreshRetryLedger(2, 10);
        RefreshQueue.Pair pair = pair(), other = pair();
        ledger.shouldRetry(pair);
        ledger.shouldRetry(other);
        ledger.forget(pair.getViewer());
        assertEquals(1, ledger.size());
        ledger.forget(other.getSubject());
        assertEquals(0, ledger.size());
    }

    @Test public void boundedCapacityRejectsUntrackedFailures() {
        RefreshRetryLedger ledger = new RefreshRetryLedger(2, 1);
        RefreshQueue.Pair one = pair(), two = pair();
        assertTrue(ledger.shouldRetry(one));
        assertFalse(ledger.shouldRetry(two));
        assertEquals(1, ledger.size());
        assertEquals(1, ledger.overflowCount());
        ledger.clear();
        assertEquals(0, ledger.overflowCount());
        assertEquals(0, ledger.exhaustedCount());
    }

    @Test public void forgettingPlayersReleasesTerminalQuarantine() {
        RefreshRetryLedger ledger = new RefreshRetryLedger(1, 2);
        RefreshQueue.Pair pair = pair();
        assertTrue(ledger.shouldRetry(pair));
        assertFalse(ledger.shouldRetry(pair));
        assertTrue(ledger.suppressed(pair));
        ledger.forget(pair.getSubject());
        assertFalse(ledger.suppressed(pair));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidCapacityRejected() { new RefreshRetryLedger(1, 0); }

    @Test public void zeroRetryBudgetCountsEachExhaustedTransitionOnce() {
        RefreshRetryLedger ledger = new RefreshRetryLedger(0, 4);
        RefreshQueue.Pair pair = pair();
        for (int i = 0; i < 100; i++) assertFalse(ledger.shouldRetry(pair));
        assertEquals(1, ledger.exhaustedCount());
        assertEquals(0, ledger.size());
        ledger.newTransition(pair);
        assertFalse(ledger.shouldRetry(pair));
        assertEquals(2, ledger.exhaustedCount());
    }
}
