package dev.fyre.disguise.skin;

import dev.fyre.disguise.api.SkinProperty;
import java.io.IOException;
import java.util.UUID;

/** Blocking texture resolver. Call on a worker thread, never on the tick thread. */
public interface SkinSource {
    SkinProperty load(UUID playerId) throws IOException;
}
