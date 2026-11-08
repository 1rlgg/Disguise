package dev.fyre.disguise.render;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Main-thread-only bounded retry ledger for failed player-info / entity sends.
 * Does not schedule timers or retry offline/vanished players on its own.
 */
public final class RefreshRetryLedger {
    private final int maxRetries;
    private final int capacity;
    private long exhausted;
    private long overflow;
    private final Map<RefreshQueue.Pair, Integer> failures =
            new HashMap<RefreshQueue.Pair, Integer>();

    public RefreshRetryLedger(int maxRetries, int capacity) {
        if (maxRetries < 0 || capacity < 1) throw new IllegalArgumentException("Invalid retry limits");
        this.maxRetries = maxRetries;
        this.capacity = capacity;
    }

    /**
     * True only while another retry is allowed. After exhaustion remove state
     * to avoid retaining player identifiers across failed refresh lifecycles.
     */
    public boolean shouldRetry(RefreshQueue.Pair pair) {
        Objects.requireNonNull(pair, "pair");
        Integer prior = failures.get(pair);
        int count = prior == null ? 1 : prior + 1;
        if (count > maxRetries) {
            failures.remove(pair);
            exhausted++;
            return false;
        }
        if (prior == null && failures.size() >= capacity) {
            overflow++;
            return false;
        }
        failures.put(pair, count);
        return true;
    }

    public void succeeded(RefreshQueue.Pair pair) {
        failures.remove(Objects.requireNonNull(pair, "pair"));
    }

    public void forget(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        failures.keySet().removeIf(pair -> playerId.equals(pair.getViewer())
                || playerId.equals(pair.getSubject()));
    }

    public int size() { return failures.size(); }
    public long exhaustedCount() { return exhausted; }
    public long overflowCount() { return overflow; }
    public void clear() { failures.clear(); exhausted = 0; overflow = 0; }
}
