package dev.fyre.disguise.skin;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Bounded, short-lived backoff for failed upstream skin lookups.
 * No errors or player details are retained, only UUIDs and monotonic times.
 */
public final class SkinFailureBackoff {
    private final int capacity;
    private final long durationNanos;
    private final LongSupplier clock;
    private final LinkedHashMap<UUID, Long> failures = new LinkedHashMap<UUID, Long>(16, 0.75f, true);

    public SkinFailureBackoff(int capacity, long durationNanos, LongSupplier clock) {
        if (capacity < 1 || durationNanos < 1) throw new IllegalArgumentException("Invalid backoff limits");
        this.capacity = capacity;
        this.durationNanos = durationNanos;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public static SkinFailureBackoff defaults() {
        return new SkinFailureBackoff(512, TimeUnit.SECONDS.toNanos(5), System::nanoTime);
    }

    public synchronized boolean blocked(UUID id) {
        Objects.requireNonNull(id, "id");
        Long since = failures.get(id);
        if (since == null) return false;
        if (clock.getAsLong() - since.longValue() >= durationNanos) {
            failures.remove(id);
            return false;
        }
        return true;
    }

    public synchronized void failed(UUID id) {
        failures.put(Objects.requireNonNull(id, "id"), clock.getAsLong());
        while (failures.size() > capacity) {
            Iterator<Map.Entry<UUID, Long>> it = failures.entrySet().iterator();
            it.next();
            it.remove();
        }
    }

    public synchronized void succeeded(UUID id) {
        failures.remove(Objects.requireNonNull(id, "id"));
    }

    public synchronized int size() { return failures.size(); }
    public synchronized void clear() { failures.clear(); }
}
