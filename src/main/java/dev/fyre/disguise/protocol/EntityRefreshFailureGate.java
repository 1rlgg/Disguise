package dev.fyre.disguise.protocol;

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
