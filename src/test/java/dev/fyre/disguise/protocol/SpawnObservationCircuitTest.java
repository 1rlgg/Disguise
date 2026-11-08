package dev.fyre.disguise.protocol;

import org.junit.Test;
import static org.junit.Assert.*;

public class SpawnObservationCircuitTest {
    @Test public void oneFailureStopsFutureObservation() {
        SpawnObservationCircuit circuit = new SpawnObservationCircuit();
        assertTrue(circuit.enabled());
        assertTrue(circuit.trip());
        assertFalse(circuit.enabled());
        assertFalse(circuit.trip());
        assertFalse(circuit.enabled());
    }
}
