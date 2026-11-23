package dev.fyre.disguise.skin;

import dev.fyre.disguise.api.SkinProperty;
import java.io.IOException;
import java.util.UUID;

public interface SkinSource {
    SkinProperty load(UUID playerId) throws IOException;
}
