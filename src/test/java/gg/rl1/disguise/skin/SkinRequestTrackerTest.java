package gg.rl1.disguise.skin;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class SkinRequestTrackerTest {
    @Test public void supersededCallbackCannotConsumeNewerToken() {
        SkinRequestTracker tracker = new SkinRequestTracker(1);
        UUID viewer = UUID.randomUUID(), subject = UUID.randomUUID();
        long first = tracker.begin(viewer, subject);
        long second = tracker.begin(viewer, subject);
        assertFalse(tracker.isCurrent(viewer, subject, first));
        tracker.finish(viewer, subject, first);
        assertTrue(tracker.isCurrent(viewer, subject, second));
        assertEquals(1, tracker.size());
        tracker.finish(viewer, subject, second);
        assertEquals(0, tracker.size());
    }

    @Test public void emergencyResetInvalidatesPendingCallbacksAcrossAllScopes() {
        SkinRequestTracker tracker = new SkinRequestTracker(4);
        UUID subject = UUID.randomUUID(), viewer = UUID.randomUUID();
        long global = tracker.begin(null, subject);
        long privateRequest = tracker.begin(viewer, subject);
        tracker.clear();
        assertEquals(0, tracker.size());
        assertFalse(tracker.isCurrent(null, subject, global));
        assertFalse(tracker.isCurrent(viewer, subject, privateRequest));
        long fresh = tracker.begin(null, subject);
        tracker.finish(null, subject, global);
        assertTrue(tracker.isCurrent(null, subject, fresh));
    }

    @Test public void latestRequestWinsOutOfOrderCompletion() {
        SkinRequestTracker tracker = new SkinRequestTracker(2);
        UUID subject = UUID.randomUUID();
        long first = tracker.begin(null, subject);
        long second = tracker.begin(null, subject);
        assertFalse(tracker.isCurrent(null, subject, first));
        assertTrue(tracker.isCurrent(null, subject, second));
        tracker.finish(null, subject, first);
        assertTrue(tracker.isCurrent(null, subject, second));
        tracker.finish(null, subject, second);
        assertFalse(tracker.isCurrent(null, subject, second));
    }

    @Test public void globalAndViewerRequestsAreIndependent() {
        SkinRequestTracker tracker = new SkinRequestTracker(2);
        UUID subject = UUID.randomUUID(), viewer = UUID.randomUUID();
        long global = tracker.begin(null, subject);
        long privateRequest = tracker.begin(viewer, subject);
        assertTrue(tracker.isCurrent(null, subject, global));
        assertTrue(tracker.isCurrent(viewer, subject, privateRequest));
        tracker.forgetPlayer(viewer);
        assertFalse(tracker.isCurrent(viewer, subject, privateRequest));
        assertTrue(tracker.isCurrent(null, subject, global));
    }

    @Test public void boundedPendingTargetsButAllowsReplacingOwnRequest() {
        SkinRequestTracker tracker = new SkinRequestTracker(1);
        UUID first = UUID.randomUUID(), other = UUID.randomUUID();
        tracker.begin(null, first);
        try {
            tracker.begin(null, other);
            fail("Expected overload rejection");
        } catch (IllegalStateException expected) {
            assertEquals(1, tracker.size());
        }
        assertTrue(tracker.isCurrent(null, first, tracker.begin(null, first)));
    }

    @Test public void forgetSubjectRejectsLateCallbacks() {
        SkinRequestTracker tracker = new SkinRequestTracker(3);
        UUID subject = UUID.randomUUID(), viewer = UUID.randomUUID();
        long token = tracker.begin(viewer, subject);
        tracker.forgetPlayer(subject);
        assertFalse(tracker.isCurrent(viewer, subject, token));
        assertEquals(0, tracker.size());
    }
}
