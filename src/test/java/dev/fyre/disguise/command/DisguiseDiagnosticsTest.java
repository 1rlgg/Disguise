package dev.fyre.disguise.command;

import org.junit.Test;
import static org.junit.Assert.*;

public class DisguiseDiagnosticsTest {
    @Test public void exposesQueueStateWithoutClaimingWorkingDisguises() {
        DisguiseDiagnostics stats = new DisguiseDiagnostics(true, false, 4, 2, 8, 1);
        assertTrue(stats.lines().get(0).contains("ON"));
        assertTrue(stats.lines().get(1).contains("OFF"));
        assertTrue(stats.lines().get(2).contains("4"));
        assertTrue(stats.lines().get(3).contains("8"));
        assertTrue(stats.lines().get(4).contains("do not prove"));
    }

    @Test public void exposesCriticalDropsAndCircuitBreaker() {
        DisguiseDiagnostics report = new DisguiseDiagnostics(true, true,
                12, 4, 2, 3, 5, 17L, 2L, true);
        assertTrue(report.lines().get(5).contains("5"));
        assertTrue(report.lines().get(6).contains("17"));
        assertTrue(report.lines().get(6).contains("2"));
        assertTrue(report.lines().get(7).contains("OPEN"));
    }

    @Test public void displaysRetryExhaustionAndCapacityFailures() {
        DisguiseDiagnostics report = new DisguiseDiagnostics(true, true,
                2, 1, 2, 0, 1, 0, 0, false, 2, 7, 3);
        assertTrue(report.lines().get(8).contains("Pending refresh retries: 2"));
        assertTrue(report.lines().get(8).contains("exhausted: 7"));
        assertTrue(report.lines().get(8).contains("capacity: 3"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesEntityWithoutTab() {
        new DisguiseDiagnostics(false, true, 0, 0, 0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesNegativeStats() {
        new DisguiseDiagnostics(false, false, -1, 0, 0, 0);
    }
}
