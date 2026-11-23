package dev.fyre.disguise.skin;

import dev.fyre.disguise.api.SkinProperty;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
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

    @Test public void sharesSimultaneousNetworkTaskWithoutSharingCancellation() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1), proceed = new CountDownLatch(1);
        SkinSource source = id -> {
            calls.incrementAndGet();
            entered.countDown();
            try {
                if (!proceed.await(2, TimeUnit.SECONDS)) throw new IOException("timeout");
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
            return new SkinProperty("dGVzdA==", "c2ln");
        };
        try (AsyncSkinService service = new AsyncSkinService(source,
                new SkinCache(10, TimeUnit.MINUTES.toNanos(1)), 1, 2)) {
            UUID id = UUID.randomUUID();
            CompletableFuture<SkinProperty> first = service.lookup(id);
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            CompletableFuture<SkinProperty> second = service.lookup(id);
            first.cancel(true);
            assertEquals(1, service.pendingLookups());
            proceed.countDown();
            assertNotNull(second.get(2, TimeUnit.SECONDS));
            assertEquals(1, calls.get());
            assertEquals(0, service.pendingLookups());
        } finally {
            proceed.countDown();
        }
    }

    @Test public void closeCompletesOutstandingFutureExceptionally() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        SkinSource source = id -> {
            entered.countDown();
            try { release.await(3, TimeUnit.SECONDS); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IOException(ex); }
            return new SkinProperty("dGVzdA==", "c2ln");
        };
        AsyncSkinService service = new AsyncSkinService(source,
                new SkinCache(10, TimeUnit.MINUTES.toNanos(1)), 1, 2);
        try {
            CompletableFuture<SkinProperty> future = service.lookup(UUID.randomUUID());
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            service.close();
            try {
                future.get(2, TimeUnit.SECONDS);
                fail("Closed service should fail pending lookup");
            } catch (ExecutionException expected) {
                assertTrue(expected.getCause() instanceof IllegalStateException);
            }
            assertEquals(0, service.pendingLookups());
        } finally { release.countDown(); service.close(); }
    }

    @Test public void failingSourceIsRateLimitedUntilCooldownExpires() throws Exception {
        java.util.concurrent.atomic.AtomicLong clock = new java.util.concurrent.atomic.AtomicLong(0);
        AtomicInteger calls = new AtomicInteger();
        SkinSource source = uuid -> {
            calls.incrementAndGet();
            throw new IOException("Mojang unavailable");
        };
        SkinFailureBackoff cooldown = new SkinFailureBackoff(8, 100, clock::get);
        try (AsyncSkinService service = new AsyncSkinService(source,
                new SkinCache(10, TimeUnit.MINUTES.toNanos(1)), 1, 2, cooldown)) {
            UUID id = UUID.randomUUID();
            try {
                service.lookup(id).get(3, TimeUnit.SECONDS);
                fail("Expected failure");
            } catch (ExecutionException expected) {
                assertTrue(expected.getCause() instanceof IOException);
            }
            assertEquals(1, calls.get());
            try {
                service.lookup(id).get(3, TimeUnit.SECONDS);
                fail("Expected short-term throttle");
            } catch (ExecutionException expected) {
                assertTrue(expected.getCause() instanceof IllegalStateException);
            }
            assertEquals(1, calls.get());
            clock.set(100);
            try {
                service.lookup(id).get(3, TimeUnit.SECONDS);
                fail("Expected retry to contact source and fail");
            } catch (ExecutionException expected) {
                assertTrue(expected.getCause() instanceof IOException);
            }
            assertEquals(2, calls.get());
        }
    }

    @Test public void unexpectedSourceErrorsStillSettleAndReleaseInFlightSlot() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        SkinSource source = uuid -> {
            if (calls.incrementAndGet() == 1) throw new AssertionError("bad provider");
            return new SkinProperty("dGVzdA==", "c2ln");
        };
        try (AsyncSkinService service = new AsyncSkinService(source,
                new SkinCache(10, TimeUnit.MINUTES.toNanos(1)), 1, 2,
                new SkinFailureBackoff(10, 1, () -> 0L))) {
            UUID id = UUID.randomUUID();
            try {
                service.lookup(id).get(2, TimeUnit.SECONDS);
                fail("Unexpected provider failure must be forwarded");
            } catch (ExecutionException expected) {
                assertTrue(expected.getCause() instanceof AssertionError);
            }
            assertEquals(0, service.pendingLookups());

            assertNotNull(service.lookup(UUID.randomUUID()).get(2, TimeUnit.SECONDS));
            assertEquals(2, calls.get());
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
