package dev.fyre.disguise.protocol;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpawnSnapshotExpiryTest {
    @Test public void onlyExpiresAfterLimit() {
        AtomicLong time = new AtomicLong(100);
        SpawnSnapshotExpiry expiry = new SpawnSnapshotExpiry(10, time::get);
        assertFalse(expiry.expired(100));
        time.set(109);
        assertFalse(expiry.expired(100));
        time.set(110);
        assertTrue(expiry.expired(100));
    }
    @Test public void handlesNanoTimeWrap() {
        AtomicLong time = new AtomicLong(Long.MAX_VALUE - 2);
        SpawnSnapshotExpiry expiry = new SpawnSnapshotExpiry(5, time::get);
        long captured = time.get();
        time.set(Long.MIN_VALUE + 2);
        assertTrue(expiry.expired(captured));
    }
    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidTtl() { new SpawnSnapshotExpiry(0, System::nanoTime); }
}
