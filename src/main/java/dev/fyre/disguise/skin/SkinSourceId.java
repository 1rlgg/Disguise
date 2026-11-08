package dev.fyre.disguise.skin;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Strict UUID parser, not an offline-player name lookup. */
public final class SkinSourceId {
    private SkinSourceId() { }

    public static Optional<UUID> parse(String input) {
        Objects.requireNonNull(input, "input");
        String raw = input.replace("-", "");
        if (!raw.matches("[0-9A-Fa-f]{32}")) return Optional.empty();
        String formatted = raw.substring(0, 8) + "-" + raw.substring(8, 12) + "-"
                + raw.substring(12, 16) + "-" + raw.substring(16, 20) + "-" + raw.substring(20);
        return Optional.of(UUID.fromString(formatted.toLowerCase(Locale.ROOT)));
    }
}
