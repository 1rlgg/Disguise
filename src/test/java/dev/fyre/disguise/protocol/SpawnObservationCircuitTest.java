package dev.fyre.disguise.protocol;

import dev.fyre.disguise.protocol.spawn.SpawnPacketObserver;

import org.junit.Test;
import static org.junit.Assert.*;

public class SpawnObservationCircuitTest {
    @Test public void oneFailureStopsFutureObservation() {
        SpawnPacketObserver.ObservationCircuit circuit = new SpawnPacketObserver.ObservationCircuit();
        assertTrue(circuit.enabled());
        assertTrue(circuit.trip());
        assertFalse(circuit.enabled());
        assertFalse(circuit.trip());
        assertFalse(circuit.enabled());
    }
}
