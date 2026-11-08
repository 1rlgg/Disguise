package dev.fyre.disguise.skin;

import dev.fyre.disguise.api.SkinProperty;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class AsyncSkinServiceTest {
    @Test public void retrievesThenCachesSignedTextures() throws Exception {
        AtomicInteger lookups = new AtomicInteger();
        SkinProperty value = new SkinProperty("dGVzdA==", "c2ln");
        SkinSource source = uuid -> { lookups.incrementAndGet(); return value; };
        try (AsyncSkinService service = new AsyncSkinService(source,
                new SkinCache(10, TimeUnit.MINUTES.toNanos(1)), 1, 2)) {
            UUID id = UUID.randomUUID();
            assertSame(value, service.lookup(id).get(2, TimeUnit.SECONDS));
            assertSame(value, service.lookup(id).get(2, TimeUnit.SECONDS));
            assertEquals(1, lookups.get());
        }
    }

    @Test public void surfacesNetworkFailures() throws Exception {
        SkinSource source = uuid -> { throw new IOException("offline"); };
        try (AsyncSkinService service = new AsyncSkinService(source,
                new SkinCache(10, TimeUnit.MINUTES.toNanos(1)), 1, 2)) {
            try {
                service.lookup(UUID.randomUUID()).get(2, TimeUnit.SECONDS);
                fail("Expected failed lookup");
            } catch (ExecutionException expected) {
                assertTrue(expected.getCause() instanceof IOException);
            }
        }
    }
}
