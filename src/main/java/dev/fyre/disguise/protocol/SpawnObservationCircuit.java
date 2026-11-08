package dev.fyre.disguise.protocol;

import java.util.concurrent.atomic.AtomicBoolean;

/** One-way circuit breaker for incompatible legacy spawn packet reflection. */
public final class SpawnObservationCircuit {
    private final AtomicBoolean tripped = new AtomicBoolean();
    public boolean enabled() { return !tripped.get(); }
    /** True on the first failure only. */
    public boolean trip() { return tripped.compareAndSet(false, true); }
}
