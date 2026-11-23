package dev.fyre.disguise.render;

public final class RefreshWorkMeter {
    private long flushes;
    private long totalPairs;
    private long totalNanos;
    private long peakNanos;

    public void record(int pairs, long elapsedNanos) {
        if (pairs < 0 || elapsedNanos < 0)
            throw new IllegalArgumentException("Refresh telemetry cannot be negative");
        flushes++;
        totalPairs += pairs;
        totalNanos += elapsedNanos;
        peakNanos = Math.max(peakNanos, elapsedNanos);
    }

    public long flushes() { return flushes; }
    public long pairs() { return totalPairs; }
    public long peakMicros() { return peakNanos / 1000L; }
    public long averageMicros() { return flushes == 0 ? 0L : (totalNanos / flushes) / 1000L; }
    public void reset() {
        flushes = 0; totalPairs = 0; totalNanos = 0; peakNanos = 0;
    }
}
