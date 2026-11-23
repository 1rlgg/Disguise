package dev.fyre.disguise.skin;

import dev.fyre.disguise.api.SkinProperty;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;


public final class SkinCache {
    private static final class Entry {
        final SkinProperty skin;
        final long storedAt;
        Entry(SkinProperty skin, long storedAt) {
            this.skin = skin;
            this.storedAt = storedAt;
        }
    }

    private final int maxEntries;
    private final long ttlNanos;
    private final LongSupplier ticker;
    private final LinkedHashMap<UUID, Entry> entries =
            new LinkedHashMap<UUID, Entry>(16, 0.75f, true);

    public SkinCache(int maxEntries, long ttlNanos) {
        this(maxEntries, ttlNanos, System::nanoTime);
    }

    public SkinCache(int maxEntries, long ttlNanos, LongSupplier ticker) {
        if (maxEntries < 1 || ttlNanos < 1) throw new IllegalArgumentException("Cache limits must be positive");
        this.maxEntries = maxEntries;
        this.ttlNanos = ttlNanos;
        this.ticker = Objects.requireNonNull(ticker, "ticker");
    }

    public synchronized Optional<SkinProperty> get(UUID id) {
        Entry entry = entries.get(Objects.requireNonNull(id, "id"));
        if (entry == null) return Optional.empty();
        if (ticker.getAsLong() - entry.storedAt >= ttlNanos) {
            entries.remove(id);
            return Optional.empty();
        }
        return Optional.of(entry.skin);
    }

    public synchronized void put(UUID id, SkinProperty skin) {
        entries.put(Objects.requireNonNull(id, "id"),
                new Entry(Objects.requireNonNull(skin, "skin"), ticker.getAsLong()));
        if (entries.size() > maxEntries) {
            Iterator<Map.Entry<UUID, Entry>> iterator = entries.entrySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    public synchronized int size() { return entries.size(); }
    public synchronized void clear() { entries.clear(); }
}
