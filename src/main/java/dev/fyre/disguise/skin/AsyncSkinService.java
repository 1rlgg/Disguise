package dev.fyre.disguise.skin;

import dev.fyre.disguise.api.SkinProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;

/**
 * Bounded single-flight resolver. Concurrent requests for the same UUID share
 * one I/O task, but each caller receives an independent cancellation handle.
 * Callbacks never execute while the service monitor is held.
 */
public final class AsyncSkinService implements AutoCloseable {
    private final SkinSource source;
    private final SkinCache cache;
    private final ThreadPoolExecutor executor;
    private final Map<UUID, CompletableFuture<SkinProperty>> inFlight =
            new HashMap<UUID, CompletableFuture<SkinProperty>>();
    private boolean closed;

    public AsyncSkinService(SkinSource source, SkinCache cache, int workers, int queueCapacity) {
        this.source = Objects.requireNonNull(source, "source");
        this.cache = Objects.requireNonNull(cache, "cache");
        if (workers < 1 || workers > 8 || queueCapacity < 1 || queueCapacity > 10000) {
            throw new IllegalArgumentException("Invalid worker or queue limits");
        }
        this.executor = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(queueCapacity), task -> {
                    Thread thread = new Thread(task, "FyreDisguise-skin-lookup");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    public CompletableFuture<SkinProperty> lookup(UUID id) {
        Objects.requireNonNull(id, "id");
        synchronized (this) {
            if (closed) return failed(new IllegalStateException("Skin service is closed"));
            java.util.Optional<SkinProperty> cached = cache.get(id);
            if (cached.isPresent()) return CompletableFuture.completedFuture(cached.get());
            CompletableFuture<SkinProperty> existing = inFlight.get(id);
            if (existing != null) return existing.thenApply(value -> value);
            CompletableFuture<SkinProperty> shared = new CompletableFuture<SkinProperty>();
            inFlight.put(id, shared);
            try {
                executor.execute(() -> resolve(id, shared));
            } catch (RejectedExecutionException ex) {
                inFlight.remove(id);
                // Use a failed shared future without exposing it directly to callers.
                shared.completeExceptionally(ex);
            }
            return shared.thenApply(value -> value);
        }
    }

    private void resolve(UUID id, CompletableFuture<SkinProperty> shared) {
        SkinProperty skin = null;
        Throwable failure = null;
        try {
            skin = source.load(id);
            if (skin == null || !skin.isSigned()) throw new IllegalStateException("Missing signed textures");
        } catch (Exception ex) {
            failure = ex;
        }
        boolean stopped;
        synchronized (this) {
            inFlight.remove(id, shared);
            stopped = closed;
            if (!stopped && failure == null) cache.put(id, skin);
        }
        // Complete downstream stages after releasing the lock: user callbacks may be slow.
        if (stopped) shared.completeExceptionally(new IllegalStateException("Skin service closed"));
        else if (failure != null) shared.completeExceptionally(failure);
        else shared.complete(skin);
    }

    private static <T> CompletableFuture<T> failed(Throwable failure) {
        CompletableFuture<T> future = new CompletableFuture<T>();
        future.completeExceptionally(failure);
        return future;
    }

    public synchronized int pendingLookups() { return inFlight.size(); }

    @Override public void close() {
        java.util.List<CompletableFuture<SkinProperty>> pending;
        synchronized (this) {
            if (closed) return;
            closed = true;
            executor.shutdownNow();
            pending = new ArrayList<CompletableFuture<SkinProperty>>(inFlight.values());
            inFlight.clear();
            cache.clear();
        }
        for (CompletableFuture<SkinProperty> future : pending) {
            future.completeExceptionally(new IllegalStateException("Skin service closed"));
        }
    }
}
