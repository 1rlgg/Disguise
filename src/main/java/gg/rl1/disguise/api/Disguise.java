package gg.rl1.disguise.api;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Disguise {
    private final UUID playerId;
    private final String name;
    private final SkinProperty skin;

    public Disguise(UUID playerId, String name) {
        this(playerId, name, null);
    }

    public Disguise(UUID playerId, String name, SkinProperty skin) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(name, "name");
        if (!validName(name)) {
            throw new IllegalArgumentException("Name must be 3-16 Minecraft username characters");
        }
        this.name = name;
        this.skin = skin;
    }

    private static boolean validName(String name) {
        if (name.length() < 3 || name.length() > 16) return false;
        for (int i = 0; i < name.length(); i++) {
            char value = name.charAt(i);
            if ((value < 'A' || value > 'Z')
                    && (value < 'a' || value > 'z')
                    && (value < '0' || value > '9')
                    && value != '_') return false;
        }
        return true;
    }

    public UUID getPlayerId() { return playerId; }
    public String getName() { return name; }
    public Optional<SkinProperty> getSkin() { return Optional.ofNullable(skin); }
}
