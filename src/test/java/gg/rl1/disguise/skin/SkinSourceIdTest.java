package gg.rl1.disguise.skin;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class SkinSourceIdTest {
    @Test public void supportsStandardAndCompactIdentifiers() {
        UUID id = UUID.randomUUID();
        assertEquals(id, MojangSkinSource.parse(id.toString()).get());
        assertEquals(id, MojangSkinSource.parse(id.toString().replace("-", "")).get());
        assertEquals(id, MojangSkinSource.parse(id.toString().toUpperCase(java.util.Locale.ROOT)).get());
    }

    @Test public void rejectsAmbiguousOrMalformedValues() {
        assertFalse(MojangSkinSource.parse("Steve").isPresent());
        assertFalse(MojangSkinSource.parse("1234").isPresent());
        assertFalse(MojangSkinSource.parse("0000000000000000000000000000000X").isPresent());
        assertFalse(MojangSkinSource.parse("00000000-0000-0000-0000-000000000000extra").isPresent());
    }

    @Test public void rejectsMisplacedSeparators() {
        assertFalse(MojangSkinSource.parse("-00000000000000000000000000000000").isPresent());
        assertFalse(MojangSkinSource.parse("0000000-00000-0000-0000-000000000000").isPresent());
        assertFalse(MojangSkinSource.parse("00000000-0000-0000-0000-000000000000-").isPresent());
    }
}
