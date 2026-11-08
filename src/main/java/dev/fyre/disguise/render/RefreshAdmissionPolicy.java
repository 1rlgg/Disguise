package dev.fyre.disguise.render;

/** Main-thread decision for work dequeued after retry exhaustion. */
public final class RefreshAdmissionPolicy {
    private RefreshAdmissionPolicy() { }

    public static boolean mayAttempt(boolean retryQuarantined) {
        return !retryQuarantined;
    }
}
