package dev.fyre.disguise.protocol;
import java.util.Objects;
import java.util.function.LongSupplier;

public final class SpawnSnapshotExpiry {
    private final long ttl;
    private final LongSupplier clock;
    public SpawnSnapshotExpiry(long ttl, LongSupplier clock) {
        if (ttl < 1) throw new IllegalArgumentException("ttl must be positive");
        this.ttl = ttl;
        this.clock = Objects.requireNonNull(clock, "clock");
    }
    public long now() { return clock.getAsLong(); }
    public boolean expired(long captured) { return now() - captured >= ttl; }
}
