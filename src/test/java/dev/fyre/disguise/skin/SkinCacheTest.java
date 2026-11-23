package dev.fyre.disguise.skin;

import dev.fyre.disguise.skin.cache.SkinCache;

import dev.fyre.disguise.api.SkinProperty;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import static org.junit.Assert.*;

public class SkinCacheTest {
    private static SkinProperty sample() { return new SkinProperty("dGVzdA==", "c2ln"); }

    @Test public void evictsLeastRecentlyUsedEntry() {
        AtomicLong now = new AtomicLong(0);
        SkinCache cache = new SkinCache(2, 100, now::get);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        cache.put(a, sample());
        cache.put(b, sample());
        assertTrue(cache.get(a).isPresent());
        cache.put(c, sample());
        assertFalse(cache.get(b).isPresent());
        assertEquals(2, cache.size());
    }

    @Test public void expiresEntries() {
        AtomicLong now = new AtomicLong(0);
        SkinCache cache = new SkinCache(1, 10, now::get);
        UUID player = UUID.randomUUID();
        cache.put(player, sample());
        now.set(10);
        assertFalse(cache.get(player).isPresent());
        assertEquals(0, cache.size());
    }
}
