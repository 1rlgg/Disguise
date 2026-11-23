package dev.fyre.disguise.protocol;

import dev.fyre.disguise.protocol.packet.TabRefreshTransaction;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class TabRefreshTransactionTest {
    @Test public void successfulRefreshUsesRemoveThenAlias() throws Exception {
        List<String> sent = new ArrayList<String>();
        TabRefreshTransaction.execute("remove", "alias", "original", sent::add);
        assertEquals(Arrays.asList("remove", "alias"), sent);
    }

    @Test public void failedRemoveStillAttemptsOriginal() {
        List<String> sent = new ArrayList<String>();
        try {
            TabRefreshTransaction.execute("remove", "alias", "original", entry -> {
                sent.add(entry);
                if ("remove".equals(entry)) throw new IllegalStateException("network failure");
            });
            fail("Must propagate failure");
        } catch (IllegalStateException expected) {
            assertEquals(Arrays.asList("remove", "remove", "original"), sent);
        } catch (InvocationTargetException unexpected) {
            fail("Wrong exception");
        }
    }

    @Test public void failedAliasRestoresOriginalAndPreservesRecoveryError() {
        List<String> sent = new ArrayList<String>();
        try {
            TabRefreshTransaction.execute("remove", "alias", "original", entry -> {
                sent.add(entry);
                if ("alias".equals(entry)) throw new IllegalStateException("first");
                if ("original".equals(entry)) throw new IllegalArgumentException("restore");
            });
            fail("Must propagate failure");
        } catch (IllegalStateException expected) {
            assertEquals(Arrays.asList("remove", "alias", "remove", "original"), sent);
            assertEquals(1, expected.getSuppressed().length);
            assertEquals("restore", expected.getSuppressed()[0].getMessage());
        } catch (InvocationTargetException unexpected) {
            fail("Wrong exception");
        }
    }

    @Test public void everyRecoveryFailureIsPreservedInOrder() {
        List<String> calls = new ArrayList<String>();
        final int[] removed = {0};
        try {
            TabRefreshTransaction.execute("remove", "alias", "original", packet -> {
                calls.add(packet);
                if ("alias".equals(packet))
                    throw new InvocationTargetException(new Exception("alias failure"));
                if ("remove".equals(packet) && ++removed[0] == 2)
                    throw new InvocationTargetException(new Exception("cleanup failure"));
                if ("original".equals(packet))
                    throw new IllegalStateException("original failure");
            });
            fail("Original failure must propagate");
        } catch (InvocationTargetException expected) {
            assertEquals(Arrays.asList("remove", "alias", "remove", "original"), calls);
            assertEquals(2, expected.getSuppressed().length);
            assertEquals("cleanup failure", expected.getSuppressed()[0].getCause().getMessage());
            assertEquals("original failure", expected.getSuppressed()[1].getMessage());
        }
    }

    @Test public void secondaryRemoveFailureDoesNotAbortOriginalProfileRestoration() {
        List<String> sent = new ArrayList<String>();
        final int[] removes = {0};
        try {
            TabRefreshTransaction.execute("remove", "alias", "original", packet -> {
                sent.add(packet);
                if ("alias".equals(packet)) throw new IllegalStateException("alias send");
                if ("remove".equals(packet) && ++removes[0] == 2)
                    throw new IllegalArgumentException("cleanup remove");
            });
            fail("Must propagate the alias failure");
        } catch (IllegalStateException expected) {
            assertEquals(Arrays.asList("remove", "alias", "remove", "original"), sent);
            assertEquals(1, expected.getSuppressed().length);
            assertEquals("cleanup remove", expected.getSuppressed()[0].getMessage());
        } catch (InvocationTargetException unexpected) {
            fail("Wrong exception");
        }
    }
}
