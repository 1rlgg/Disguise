package gg.rl1.disguise.skin;

import gg.rl1.disguise.api.SkinProperty;
import java.io.IOException;
import java.util.UUID;

public interface SkinSource {
    SkinProperty load(UUID playerId) throws IOException;
}
