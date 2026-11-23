package dev.fyre.disguise.skin;

import dev.fyre.disguise.api.SkinProperty;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;


public class SkinCallbackTest {
    @Test public void callbackCanReadServiceStateAndStartNextLookup() throws Exception {
        SkinProperty skin = new SkinProperty("dGVzdA==", "c2ln");
        try (AsyncSkinService service = new AsyncSkinService(
                id -> skin, new SkinCache(8, TimeUnit.MINUTES.toNanos(1)), 1, 4)) {
            CountDownLatch callback = new CountDownLatch(1);
            UUID first = UUID.randomUUID();
            service.lookup(first).thenAccept(value -> {
                assertSame(skin, value);
                assertTrue(service.pendingLookups() >= 0);
                service.lookup(UUID.randomUUID());
                callback.countDown();
            });
            assertTrue(callback.await(3, TimeUnit.SECONDS));
        }
    }

    @Test public void lookupAfterClosingFails() throws Exception {
        AsyncSkinService service = new AsyncSkinService(
                id -> new SkinProperty("dGVzdA==", "c2ln"),
                new SkinCache(8, TimeUnit.SECONDS.toNanos(1)), 1, 4);
        service.close();
        try {
            service.lookup(UUID.randomUUID()).get(2, TimeUnit.SECONDS);
            fail("Closed service must reject requests");
        } catch (java.util.concurrent.ExecutionException expected) {
            assertTrue(expected.getCause() instanceof IllegalStateException);
        }
    }
}
