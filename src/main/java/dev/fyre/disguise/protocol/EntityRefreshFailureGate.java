package dev.fyre.disguise.protocol;

/**
 * Disables optional world entity refresh after consecutive failures.
 * The tab-list-only packet path remains available.
 */
public final class EntityRefreshFailureGate {
    private final PacketRewriteBreaker breaker;
    public EntityRefreshFailureGate(int threshold) {
        breaker = new PacketRewriteBreaker(threshold);
    }
    public boolean enabled() { return !breaker.isOpen(); }
    public boolean failure() { return breaker.recordFailure(); }
    public void success() { breaker.recordSuccess(); }
    public int failures() { return breaker.failureCount(); }
}
