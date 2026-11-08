package dev.fyre.disguise.skin;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class SkinSourceIdTest {
    @Test public void supportsStandardAndCompactIdentifiers() {
        UUID id = UUID.randomUUID();
        assertEquals(id, SkinSourceId.parse(id.toString()).get());
        assertEquals(id, SkinSourceId.parse(id.toString().replace("-", "")).get());
        assertEquals(id, SkinSourceId.parse(id.toString().toUpperCase(java.util.Locale.ROOT)).get());
    }

    @Test public void rejectsAmbiguousOrMalformedValues() {
        assertFalse(SkinSourceId.parse("Steve").isPresent());
        assertFalse(SkinSourceId.parse("1234").isPresent());
        assertFalse(SkinSourceId.parse("0000000000000000000000000000000X").isPresent());
        assertFalse(SkinSourceId.parse("00000000-0000-0000-0000-000000000000extra").isPresent());
    }
}
