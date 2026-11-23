package dev.fyre.disguise.api;

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
        if (!name.matches("[A-Za-z0-9_]{3,16}")) {
            throw new IllegalArgumentException("Name must be 3-16 Minecraft username characters");
        }
        this.name = name;
        this.skin = skin;
    }

    public UUID getPlayerId() { return playerId; }
    public String getName() { return name; }
    public Optional<SkinProperty> getSkin() { return Optional.ofNullable(skin); }
}
