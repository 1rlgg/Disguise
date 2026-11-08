package dev.fyre.disguise.skin;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import static org.junit.Assert.*;

public class SkinFailureBackoffTest {
    @Test public void respectsCooldownAndSuccessReset() {
        AtomicLong clock = new AtomicLong(100);
        SkinFailureBackoff backoff = new SkinFailureBackoff(2, 10, clock::get);
        UUID id = UUID.randomUUID();
        assertFalse(backoff.blocked(id));
        backoff.failed(id);
        assertTrue(backoff.blocked(id));
        clock.set(109);
        assertTrue(backoff.blocked(id));
        clock.set(110);
        assertFalse(backoff.blocked(id));
        backoff.failed(id);
        backoff.succeeded(id);
        assertFalse(backoff.blocked(id));
    }

    @Test public void capacityIsBoundedAndEvictsOldest() {
        AtomicLong clock = new AtomicLong(0);
        SkinFailureBackoff backoff = new SkinFailureBackoff(1, 100, clock::get);
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        backoff.failed(first);
        backoff.failed(second);
        assertEquals(1, backoff.size());
        assertFalse(backoff.blocked(first));
        assertTrue(backoff.blocked(second));
        backoff.clear();
        assertEquals(0, backoff.size());
    }

    @Test public void expiryHandlesNanoTimeWrap() {
        AtomicLong clock = new AtomicLong(Long.MAX_VALUE - 1);
        SkinFailureBackoff backoff = new SkinFailureBackoff(1, 4, clock::get);
        UUID id = UUID.randomUUID();
        backoff.failed(id);
        clock.set(Long.MIN_VALUE + 2);
        assertFalse(backoff.blocked(id));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroDuration() {
        new SkinFailureBackoff(1, 0, System::nanoTime);
    }
}
