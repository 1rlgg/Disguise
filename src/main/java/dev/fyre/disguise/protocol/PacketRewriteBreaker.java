package dev.fyre.disguise.protocol;

import java.util.concurrent.atomic.AtomicInteger;

public final class PacketRewriteBreaker {
    private final int threshold;
    private final AtomicInteger failures = new AtomicInteger();

    public PacketRewriteBreaker(int threshold) {
        if (threshold < 1) throw new IllegalArgumentException("threshold must be positive");
        this.threshold = threshold;
    }

    public boolean isOpen() { return failures.get() >= threshold; }
    public int failureCount() { return failures.get(); }

    public boolean recordFailure() {
        int previous = failures.getAndUpdate(old -> old >= threshold ? threshold : old + 1);
        return previous == threshold - 1;
    }

    public void recordSuccess() {

        if (!isOpen()) failures.set(0);
    }
}
