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

    @Test(expected = IllegalArgumentException.class)
    public void refusesEntityWithoutTab() {
        new DisguiseDiagnostics(false, true, 0, 0, 0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesNegativeStats() {
        new DisguiseDiagnostics(false, false, -1, 0, 0, 0);
    }
}
