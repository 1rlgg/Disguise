package dev.fyre.disguise.render;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Objects;
import java.util.UUID;

public final class RefreshRetryLedger {
    private final int maxRetries;
    private final int capacity;
    private final Set<RefreshQueue.Pair> terminal = new LinkedHashSet<RefreshQueue.Pair>();
    private long exhausted;
    private long overflow;
    private final Map<RefreshQueue.Pair, Integer> failures =
            new HashMap<RefreshQueue.Pair, Integer>();

    public RefreshRetryLedger(int maxRetries, int capacity) {
        if (maxRetries < 0 || capacity < 1) throw new IllegalArgumentException("Invalid retry limits");
        this.maxRetries = maxRetries;
        this.capacity = capacity;
    }

    public boolean shouldRetry(RefreshQueue.Pair pair) {
        Objects.requireNonNull(pair, "pair");
        Integer prior = failures.get(pair);
        int count = prior == null ? 1 : prior + 1;
        if (terminal.contains(pair)) return false;
        if (count > maxRetries) {
            failures.remove(pair);
            exhausted++;

            if (terminal.size() >= capacity) {
                java.util.Iterator<RefreshQueue.Pair> it = terminal.iterator();
                it.next();
                it.remove();
            }
            terminal.add(pair);
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
        terminal.remove(pair);
    }

    public boolean suppressed(RefreshQueue.Pair pair) {
        return terminal.contains(Objects.requireNonNull(pair, "pair"));
    }

    public void newTransition(RefreshQueue.Pair pair) {
        succeeded(pair);
    }

    public void forget(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        failures.keySet().removeIf(pair -> playerId.equals(pair.getViewer())
                || playerId.equals(pair.getSubject()));
        terminal.removeIf(pair -> playerId.equals(pair.getViewer())
                || playerId.equals(pair.getSubject()));
    }

    public int size() { return failures.size(); }
    public long exhaustedCount() { return exhausted; }
    public long overflowCount() { return overflow; }
    public void clear() { failures.clear(); terminal.clear(); exhausted = 0; overflow = 0; }
}
