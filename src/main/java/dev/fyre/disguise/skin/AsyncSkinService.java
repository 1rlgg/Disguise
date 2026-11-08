package dev.fyre.disguise.skin;

import dev.fyre.disguise.api.SkinProperty;
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
 * Bounded single-flight skin resolver. Concurrent requests for one UUID share a
 * network task, while callers receive independent completion stages.
 * All future callbacks execute on the resolving thread unless explicitly scheduled.
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

    public synchronized CompletableFuture<SkinProperty> lookup(UUID id) {
        Objects.requireNonNull(id, "id");
        if (closed) return failed(new IllegalStateException("Skin service is closed"));
        java.util.Optional<SkinProperty> cached = cache.get(id);
        if (cached.isPresent()) return CompletableFuture.completedFuture(cached.get());
        CompletableFuture<SkinProperty> pending = inFlight.get(id);
        if (pending != null) return pending.thenApply(value -> value);
        CompletableFuture<SkinProperty> shared = new CompletableFuture<SkinProperty>();
        inFlight.put(id, shared);
        try {
            executor.execute(() -> resolve(id, shared));
        } catch (RejectedExecutionException ex) {
            inFlight.remove(id);
            shared.completeExceptionally(ex);
        }
        return shared.thenApply(value -> value);
    }

    private void resolve(UUID id, CompletableFuture<SkinProperty> shared) {
        SkinProperty skin = null;
        Throwable error = null;
        try {
            skin = source.load(id);
            if (skin == null || !skin.isSigned()) throw new IllegalStateException("Missing signed textures");
        } catch (Exception ex) {
            error = ex;
        }
        synchronized (this) {
            inFlight.remove(id, shared);
            if (closed) {
                shared.completeExceptionally(new IllegalStateException("Skin service is closed"));
            } else if (error != null) {
                shared.completeExceptionally(error);
            } else {
                cache.put(id, skin);
                shared.complete(skin);
            }
        }
    }

    private static <T> CompletableFuture<T> failed(Throwable failure) {
        CompletableFuture<T> result = new CompletableFuture<T>();
        result.completeExceptionally(failure);
        return result;
    }

    public synchronized int pendingLookups() { return inFlight.size(); }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        executor.shutdownNow();
        for (CompletableFuture<SkinProperty> shared : inFlight.values()) {
            shared.completeExceptionally(new IllegalStateException("Skin service closed"));
        }
        inFlight.clear();
        cache.clear();
    }
}
