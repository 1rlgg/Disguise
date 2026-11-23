package dev.fyre.disguise.render;

import dev.fyre.disguise.render.refresh.RefreshWorkMeter;

import org.junit.Test;
import static org.junit.Assert.*;

public class RefreshWorkMeterTest {
    @Test public void reportsAveragePeakAndProcessedPairCounts() {
        RefreshWorkMeter meter = new RefreshWorkMeter();
        assertEquals(0, meter.averageMicros());
        meter.record(4, 1000L);
        meter.record(0, 3000L);
        assertEquals(2, meter.flushes());
        assertEquals(4, meter.pairs());
        assertEquals(2, meter.averageMicros());
        assertEquals(3, meter.peakMicros());
        meter.reset();
        assertEquals(0, meter.flushes());
        assertEquals(0, meter.peakMicros());
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesInvalidTime() { new RefreshWorkMeter().record(1, -1); }

    @Test(expected = IllegalArgumentException.class)
    public void refusesInvalidCount() { new RefreshWorkMeter().record(-1, 1); }
}
