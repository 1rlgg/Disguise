package dev.fyre.disguise.api;

import java.util.Objects;
import java.util.UUID;

/** Immutable requested disguise identity. This model does not modify client packets. */
public final class Disguise {
    private final UUID playerId;
    private final String name;

    public Disguise(UUID playerId, String name) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(name, "name");
        if (!name.matches("[A-Za-z0-9_]{3,16}")) {
            throw new IllegalArgumentException("Name must be 3-16 Minecraft username characters");
        }
        this.name = name;
    }

    public UUID getPlayerId() { return playerId; }
    public String getName() { return name; }
}
