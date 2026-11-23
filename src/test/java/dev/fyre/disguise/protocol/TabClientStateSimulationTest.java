package dev.fyre.disguise.protocol;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class TabClientStateSimulationTest {
    private static final class FakeClient {
        private final Map<UUID, String> entries = new LinkedHashMap<UUID, String>();
        private final UUID subject = UUID.randomUUID();
        private int calls;
        private int failOnCall;
        private int failAlsoOnCall;

        void send(String operation) throws InvocationTargetException {
            calls++;
            if ("remove".equals(operation)) entries.remove(subject);
            if ("alias".equals(operation)) entries.put(subject, "OtherName");
            if ("original".equals(operation)) entries.put(subject, "RealName");

            if (calls == failOnCall || calls == failAlsoOnCall)
                throw new InvocationTargetException(new Exception("ambiguous send " + calls));
        }
    }

    @Test public void successfulChangeKeepsOriginalUUIDAndExpectedAlias() throws Exception {
        FakeClient client = new FakeClient();
        client.entries.put(client.subject, "RealName");
        TabRefreshTransaction.execute("remove", "alias", "original", client::send);
        assertEquals(1, client.entries.size());
        assertEquals("OtherName", client.entries.get(client.subject));
    }

    @Test public void ambiguousFailuresAfterEitherPacketRecoverOriginalUUIDAndName() {
        for (int failingCall = 1; failingCall <= 2; failingCall++) {
            FakeClient client = new FakeClient();
            client.entries.put(client.subject, "RealName");
            client.failOnCall = failingCall;
            try {
                TabRefreshTransaction.execute("remove", "alias", "original", client::send);
                fail("Expected an ambiguous failure");
            } catch (InvocationTargetException expected) {
                assertEquals(1, client.entries.size());
                assertEquals("RealName", client.entries.get(client.subject));
                assertEquals(0, expected.getSuppressed().length);
                assertEquals(failingCall == 1 ? 3 : 4, client.calls);
            }
        }
    }

    @Test public void cleanupFailureStillAllowsOriginalProfileToReachClient() {
        FakeClient client = new FakeClient();
        client.failOnCall = 2;
        client.failAlsoOnCall = 3;
        try {
            TabRefreshTransaction.execute("remove", "alias", "original", client::send);
            fail("Original failure should propagate");
        } catch (InvocationTargetException expected) {
            assertEquals(4, client.calls);
            assertEquals(1, client.entries.size());
            assertEquals("RealName", client.entries.get(client.subject));
            assertEquals(1, expected.getSuppressed().length);
            assertEquals("ambiguous send 3",
                    expected.getSuppressed()[0].getCause().getMessage());
        }
    }

    @Test public void successfulRecoveryStillPreservesTheFirstTransportError() {
        FakeClient client = new FakeClient();
        client.failOnCall = 2;
        try {
            TabRefreshTransaction.execute("remove", "alias", "original", client::send);
            fail("Original send failure must remain observable");
        } catch (InvocationTargetException expected) {
            assertEquals("ambiguous send 2", expected.getCause().getMessage());
            assertEquals("RealName", client.entries.get(client.subject));
        }
    }
}
