package dev.fyre.disguise.protocol;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fail-open guard. When rewrite failures hit the threshold, pass original
 * packets through unchanged until the plugin is restarted.
 */
public final class PacketRewriteBreaker {
    private final int threshold;
    private final AtomicInteger failures = new AtomicInteger();

    public PacketRewriteBreaker(int threshold) {
        if (threshold < 1) throw new IllegalArgumentException("threshold must be positive");
        this.threshold = threshold;
    }

    public boolean isOpen() { return failures.get() >= threshold; }
    public int failureCount() { return failures.get(); }

    /** Returns true only on the transition to disabled. */
    public boolean recordFailure() {
        int next = failures.updateAndGet(old -> old >= threshold ? threshold : old + 1);
        return next == threshold;
    }

    public void recordSuccess() {
        // Isolated errors should not permanently disable a healthy packet path.
        if (!isOpen()) failures.set(0);
    }
}
