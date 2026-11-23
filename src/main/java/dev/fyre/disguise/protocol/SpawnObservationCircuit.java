package dev.fyre.disguise.protocol;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SpawnObservationCircuit {
    private final AtomicBoolean tripped = new AtomicBoolean();
    public boolean enabled() { return !tripped.get(); }

    public boolean trip() { return tripped.compareAndSet(false, true); }
}
