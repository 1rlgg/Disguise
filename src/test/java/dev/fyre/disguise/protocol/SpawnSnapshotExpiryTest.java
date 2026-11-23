package dev.fyre.disguise.protocol;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpawnSnapshotExpiryTest {
    @Test public void onlyExpiresAfterLimit() {
        AtomicLong time = new AtomicLong(100);
        
        assertFalse(TrackedSpawnPackets.expiredSince(100, 10, time::get));
        time.set(109);
        assertFalse(TrackedSpawnPackets.expiredSince(100, 10, time::get));
        time.set(110);
        assertTrue(TrackedSpawnPackets.expiredSince(100, 10, time::get));
    }
    @Test public void handlesNanoTimeWrap() {
        AtomicLong time = new AtomicLong(Long.MAX_VALUE - 2);
        
        long captured = time.get();
        time.set(Long.MIN_VALUE + 2);
        assertTrue(TrackedSpawnPackets.expiredSince(captured, 5, time::get));
    }
    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidTtl() { TrackedSpawnPackets.expiredSince(0, 0, System::nanoTime); }
}
