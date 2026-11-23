package dev.fyre.disguise.render;


public final class RestorationEligibility {
    private RestorationEligibility() { }

    public static boolean canRestore(boolean applied, boolean disguised,
                                     boolean viewerOnline, boolean subjectOnline,
                                     boolean visible, boolean self) {
        return applied && !disguised && viewerOnline && subjectOnline
                && visible && !self;
    }
}
