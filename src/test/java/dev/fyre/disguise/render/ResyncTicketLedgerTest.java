package dev.fyre.disguise.render;

import java.util.UUID;
import dev.fyre.disguise.FyreDisguisePlugin.ResyncTickets;
import org.junit.Test;
import static org.junit.Assert.*;

public class ResyncTicketLedgerTest {
    @Test public void aStaleJoinCannotResyncTheNextSession() {
        ResyncTickets ledger = new ResyncTickets();
        UUID player = UUID.randomUUID();
        long join1 = ledger.schedule(player);
        ledger.forget(player);
        long join2 = ledger.schedule(player);
        assertFalse(ledger.consume(player, join1));
        assertTrue(ledger.consume(player, join2));
        assertFalse(ledger.consume(player, join2));
    }

    @Test public void latestWorldChangeSupersedesEarlierDelayedTasks() {
        ResyncTickets ledger = new ResyncTickets();
        UUID player = UUID.randomUUID();
        long first = ledger.schedule(player);
        long latest = ledger.schedule(player);
        assertFalse(ledger.consume(player, first));
        assertTrue(ledger.consume(player, latest));
        assertEquals(0, ledger.pendingCount());
    }

    @Test public void rapidReconnectAndWorldChangesNeverAcceptSupersededTickets() {
        ResyncTickets ledger = new ResyncTickets();
        UUID player = UUID.randomUUID();
        for (int cycle = 0; cycle < 1000; cycle++) {
            long old = ledger.schedule(player);
            long latest = ledger.schedule(player);
            assertFalse(ledger.consume(player, old));
            if ((cycle & 1) == 0) {
                ledger.forget(player);
                assertFalse(ledger.consume(player, latest));
            } else {
                assertTrue(ledger.consume(player, latest));
            }
        }
        assertEquals(0, ledger.pendingCount());
    }

    @Test public void playerCleanupDoesNotCancelOtherPlayersResync() {
        ResyncTickets ledger = new ResyncTickets();
        UUID one = UUID.randomUUID(), two = UUID.randomUUID();
        long a = ledger.schedule(one), b = ledger.schedule(two);
        ledger.forget(one);
        assertFalse(ledger.consume(one, a));
        assertTrue(ledger.consume(two, b));
        ledger.clear();
        assertEquals(0, ledger.pendingCount());
    }
}
