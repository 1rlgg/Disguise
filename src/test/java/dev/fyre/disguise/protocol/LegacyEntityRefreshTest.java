package dev.fyre.disguise.protocol;

import org.junit.Test;
import static org.junit.Assert.*;

public class LegacyEntityRefreshTest {
    @Test public void playerSpawnHeldItemReflectsCurrentItemId() {
        assertEquals((short) 0, LegacyEntityRefresh.heldItemId(0));
        assertEquals((short) 276, LegacyEntityRefresh.heldItemId(276));
        assertEquals(Short.MAX_VALUE, LegacyEntityRefresh.heldItemId(Short.MAX_VALUE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeHeldItemId() { LegacyEntityRefresh.heldItemId(-1); }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOverflowingHeldItemId() { LegacyEntityRefresh.heldItemId(Short.MAX_VALUE + 1); }

    @Test public void legacyEquipmentSlotsAreShortAndBounded() {
        for (int slot = 0; slot <= 4; slot++) {
            assertEquals((short) slot, LegacyEntityRefresh.equipmentSlot(slot));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsLegacyEquipmentSlotOutsideArmorRange() {
        LegacyEntityRefresh.equipmentSlot(5);
    }

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
