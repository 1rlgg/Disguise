package gg.rl1.disguise.skin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class SkinRequestTracker {
    private static final class Target {
        final UUID viewer;
        final UUID subject;
        Target(UUID viewer, UUID subject) {
            this.viewer = viewer;
            this.subject = Objects.requireNonNull(subject, "subject");
        }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Target)) return false;
            Target target = (Target) other;
            return Objects.equals(viewer, target.viewer) && subject.equals(target.subject);
        }
        @Override public int hashCode() {
            return 31 * (viewer == null ? 0 : viewer.hashCode()) + subject.hashCode();
        }
    }

    private final int capacity;
    private final Map<Target, Long> latest = new HashMap<Target, Long>();
    private long sequence;
    private final Map<Target, java.util.concurrent.CompletableFuture<?>> callbacks = new HashMap<>();

    public SkinRequestTracker(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
    }

    public synchronized long begin(UUID viewer, UUID subject) {
        Target target = new Target(viewer, subject);
        if (!latest.containsKey(target) && latest.size() >= capacity) {
            throw new IllegalStateException("Too many active skin requests");
        }
        long token = ++sequence;
        latest.put(target, token);
        cancelCallback(target);
        return token;
    }

    public synchronized boolean isCurrent(UUID viewer, UUID subject, long token) {
        Long value = latest.get(new Target(viewer, subject));
        return value != null && value.longValue() == token;
    }

    public synchronized void finish(UUID viewer, UUID subject, long token) {
        Target target = new Target(viewer, subject);
        if (latest.remove(target, token)) callbacks.remove(target);
    }

    public synchronized boolean cancel(UUID viewer, UUID subject) {
        Target target = new Target(viewer, subject);
        boolean removed = latest.remove(target) != null;
        cancelCallback(target);
        return removed;
    }

    public synchronized void forgetPlayer(UUID player) {
        Objects.requireNonNull(player, "player");
        java.util.Iterator<Target> iterator = latest.keySet().iterator();
        while (iterator.hasNext()) {
            Target target = iterator.next();
            if (player.equals(target.subject) || player.equals(target.viewer)) {
                iterator.remove();
                cancelCallback(target);
            }
        }
    }

    public synchronized void attach(UUID viewer, UUID subject, long token,
                                    java.util.concurrent.CompletableFuture<?> callback) {
        Objects.requireNonNull(callback, "callback");
        Target target = new Target(viewer, subject);
        Long current = latest.get(target);
        if (current == null || current.longValue() != token) {
            callback.cancel(false);
            return;
        }
        cancelCallback(target);
        callbacks.put(target, callback);
    }

    private void cancelCallback(Target target) {
        java.util.concurrent.CompletableFuture<?> callback = callbacks.remove(target);
        if (callback != null) callback.cancel(false);
    }

    public synchronized int size() { return latest.size(); }
    public synchronized void clear() {
        latest.clear();
        for (java.util.concurrent.CompletableFuture<?> callback : callbacks.values()) callback.cancel(false);
        callbacks.clear();
    }
}
