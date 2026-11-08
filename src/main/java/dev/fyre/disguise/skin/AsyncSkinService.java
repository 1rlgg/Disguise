package dev.fyre.disguise.skin;

import dev.fyre.disguise.api.SkinProperty;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Bounded I/O executor: calls return a future and never block the Bukkit thread
 * on a network request. Completions happen on the worker thread; Bukkit actions
 * must be scheduled back onto the primary server thread.
 */
public final class AsyncSkinService implements AutoCloseable {
    private final SkinSource source;
    private final SkinCache cache;
    private final ExecutorService executor;

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
        java.util.Optional<SkinProperty> cached = cache.get(id);
        if (cached.isPresent()) return CompletableFuture.completedFuture(cached.get());
        CompletableFuture<SkinProperty> result = new CompletableFuture<SkinProperty>();
        try {
            executor.execute(() -> {
                if (result.isCancelled()) return;
                try {
                    SkinProperty skin = source.load(id);
                    if (skin == null || !skin.isSigned()) {
                        throw new IllegalStateException("Skin source returned unsigned or null textures");
                    }
                    if (!result.isCancelled()) {
                        cache.put(id, skin);
                        result.complete(skin);
                    }
                } catch (Exception ex) {
                    result.completeExceptionally(ex);
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException ex) {
            result.completeExceptionally(ex);
        }
        return result;
    }

    @Override public void close() {
        executor.shutdownNow();
        cache.clear();
    }
}
