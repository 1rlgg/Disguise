package dev.fyre.disguise.render;

/**
 * Reconciliation must not send tab packets for offline or vanished entities.
 * Keep applied bookkeeping so restoration can be reconsidered later.
 */
public final class RestorationEligibility {
    private RestorationEligibility() { }

    public static boolean canRestore(boolean applied, boolean disguised,
                                     boolean viewerOnline, boolean subjectOnline,
                                     boolean visible, boolean self) {
        return applied && !disguised && viewerOnline && subjectOnline
                && visible && !self;
    }
}
