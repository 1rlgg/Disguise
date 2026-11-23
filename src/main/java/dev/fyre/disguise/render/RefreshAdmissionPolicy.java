package dev.fyre.disguise.render;


public final class RefreshAdmissionPolicy {
    private RefreshAdmissionPolicy() { }

    public static boolean mayAttempt(boolean retryQuarantined) {
        return !retryQuarantined;
    }
}
