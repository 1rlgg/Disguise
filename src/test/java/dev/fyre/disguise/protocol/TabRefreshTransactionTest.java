package dev.fyre.disguise.protocol;

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
            assertEquals(Arrays.asList("remove", "original"), sent);
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
            assertEquals(Arrays.asList("remove", "alias", "original"), sent);
            assertEquals(1, expected.getSuppressed().length);
            assertEquals("restore", expected.getSuppressed()[0].getMessage());
        } catch (InvocationTargetException unexpected) {
            fail("Wrong exception");
        }
    }
}
