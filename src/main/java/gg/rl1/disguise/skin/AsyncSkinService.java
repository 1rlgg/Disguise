package gg.rl1.disguise.skin;

import gg.rl1.disguise.api.SkinProperty;
import gg.rl1.disguise.skin.cache.SkinCache;
import gg.rl1.disguise.skin.cache.SkinFailureBackoff;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class AsyncSkinService implements AutoCloseable {
    private final SkinSource source;
    private final SkinCache cache;
    private final SkinFailureBackoff failures;
    private final ThreadPoolExecutor executor;
    private final Map<UUID, CompletableFuture<SkinProperty>> inFlight =
            new HashMap<UUID, CompletableFuture<SkinProperty>>();
    private boolean closed;

    public AsyncSkinService(SkinSource source, SkinCache cache, int workers, int queueCapacity) {
        this(source, cache, workers, queueCapacity, SkinFailureBackoff.defaults());
    }

    AsyncSkinService(SkinSource source, SkinCache cache, int workers, int queueCapacity,
                     SkinFailureBackoff failures) {
        this.source = Objects.requireNonNull(source, "source");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.failures = Objects.requireNonNull(failures, "failures");
        if (workers < 1 || workers > 8 || queueCapacity < 1 || queueCapacity > 10000) {
            throw new IllegalArgumentException("Invalid worker or queue limits");
        }
        this.executor = new ThreadPoolExecutor(workers, workers, 30L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(queueCapacity), task -> {
                    Thread thread = new Thread(task, "Disguise-skin-lookup");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
        this.executor.allowCoreThreadTimeOut(true);
    }

    public CompletableFuture<SkinProperty> lookup(UUID id) {
        Objects.requireNonNull(id, "id");
        CompletableFuture<SkinProperty> shared;
        boolean submit = false;
        synchronized (this) {
            if (closed) return failed(new IllegalStateException("Skin service is closed"));
            Optional<SkinProperty> cached = cache.get(id);
            if (cached.isPresent()) return CompletableFuture.completedFuture(cached.get());
            shared = inFlight.get(id);
            if (shared == null) {
                if (failures.blocked(id)) {
                    return failed(new IllegalStateException("Skin lookup temporarily unavailable; retry shortly"));
                }
                shared = new CompletableFuture<SkinProperty>();
                inFlight.put(id, shared);
                submit = true;
            }
        }

        if (submit) {
            final CompletableFuture<SkinProperty> taskFuture = shared;
            try {
                executor.execute(() -> resolve(id, taskFuture));
            } catch (RejectedExecutionException ex) {
                synchronized (this) {
                    inFlight.remove(id, taskFuture);
                }
                taskFuture.completeExceptionally(ex);
            }
        }

        return shared.thenApply(value -> value);
    }

    private void resolve(UUID id, CompletableFuture<SkinProperty> shared) {
        synchronized (this) {
            if (closed || inFlight.get(id) != shared) return;
        }
        SkinProperty skin = null;
        Throwable failure = null;
        try {
            skin = source.load(id);
            if (skin == null || !skin.isSigned()) throw new IllegalStateException("Missing signed textures");
        } catch (Throwable ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            failure = ex;
        }
        boolean stopped;
        synchronized (this) {
            inFlight.remove(id, shared);
            stopped = closed;
            if (!stopped && failure == null) {
                cache.put(id, skin);
                failures.succeeded(id);
            } else if (!stopped) {
                failures.failed(id);
            }
        }

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
    public synchronized int coolingDownSources() { return failures.size(); }

    @Override public void close() {
        List<CompletableFuture<SkinProperty>> pending;
        synchronized (this) {
            if (closed) return;
            closed = true;
            pending = new ArrayList<CompletableFuture<SkinProperty>>(inFlight.values());
            inFlight.clear();
            cache.clear();
            failures.clear();
        }
        executor.shutdownNow();
        for (CompletableFuture<SkinProperty> future : pending) {
            future.completeExceptionally(new IllegalStateException("Skin service closed"));
        }
    }
}
