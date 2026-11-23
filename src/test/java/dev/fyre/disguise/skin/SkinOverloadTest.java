package dev.fyre.disguise.skin;

import dev.fyre.disguise.skin.cache.SkinCache;

import dev.fyre.disguise.api.SkinProperty;
import java.util.UUID;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

public class SkinOverloadTest {
    @Test public void rejectsOverflowWithoutDisturbingActiveRequests() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        SkinProperty skin = new SkinProperty("dGVzdA==", "c2ln");
        try (AsyncSkinService service = new AsyncSkinService(id -> {
            started.countDown();
            try {
                if (!release.await(4, TimeUnit.SECONDS)) throw new IOException("Timed out");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted", interrupted);
            }
            return skin;
        }, new SkinCache(8, TimeUnit.MINUTES.toNanos(1)), 1, 1)) {
            CompletableFuture<SkinProperty> first = service.lookup(UUID.randomUUID());
            assertTrue(started.await(2, TimeUnit.SECONDS));
            CompletableFuture<SkinProperty> queued = service.lookup(UUID.randomUUID());
            CompletableFuture<SkinProperty> overflow = service.lookup(UUID.randomUUID());
            try {
                overflow.get(2, TimeUnit.SECONDS);
                fail("Queue overflow must fail quickly");
            } catch (ExecutionException expected) {
                assertTrue(expected.getCause() instanceof RejectedExecutionException);
            } finally {
                release.countDown();
            }
            assertSame(skin, first.get(3, TimeUnit.SECONDS));
            assertSame(skin, queued.get(3, TimeUnit.SECONDS));
        }
    }

    @Test public void cancellingOneHandleDoesNotCancelSharedRequest() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        UUID id = UUID.randomUUID();
        SkinProperty skin = new SkinProperty("dGVzdA==", "c2ln");
        try (AsyncSkinService service = new AsyncSkinService(source -> {
            try {
                if (!release.await(4, TimeUnit.SECONDS)) throw new IOException("Timed out");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted", interrupted);
            }
            return skin;
        }, new SkinCache(8, TimeUnit.MINUTES.toNanos(1)), 1, 2)) {
            CompletableFuture<SkinProperty> one = service.lookup(id);
            CompletableFuture<SkinProperty> two = service.lookup(id);
            assertTrue(one.cancel(false));
            release.countDown();
            assertSame(skin, two.get(3, TimeUnit.SECONDS));
        }
    }
}
