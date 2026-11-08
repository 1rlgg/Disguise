package dev.fyre.disguise.protocol;

import org.junit.Test;
import static org.junit.Assert.*;

public class LegacyEntityRefreshTest {
    @Test public void fixedPointCoordinatesUseFloorForNegativeValues() {
        assertEquals(32, LegacyEntityRefresh.fixed(1.0));
        assertEquals(-1, LegacyEntityRefresh.fixed(-0.01));
        assertEquals(-33, LegacyEntityRefresh.fixed(-1.01));
        assertEquals(0, LegacyEntityRefresh.fixed(0));
    }

    @Test public void angleUsesUnsignedByteWrap() {
        assertEquals(0, LegacyEntityRefresh.angle(0));
        assertEquals(64, LegacyEntityRefresh.angle(90));
        assertEquals((byte) -128, LegacyEntityRefresh.angle(180));
        assertEquals((byte) -64, LegacyEntityRefresh.angle(270));
    }
}
