package dev.fyre.disguise.skin;

import dev.fyre.disguise.api.SkinProperty;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class MojangSkinSource implements SkinSource {
    private static final int MAX_BODY_CHARS = 32768;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    public MojangSkinSource(int connectTimeoutMillis, int readTimeoutMillis) {
        if (connectTimeoutMillis < 1 || readTimeoutMillis < 1) {
            throw new IllegalArgumentException("Timeouts must be positive");
        }
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override public SkinProperty load(UUID playerId) throws IOException {
        Objects.requireNonNull(playerId, "playerId");
        URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/"
                + playerId.toString().replace("-", "") + "?unsigned=false");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(connectTimeoutMillis);
        connection.setReadTimeout(readTimeoutMillis);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/json");
        try {
            int code = connection.getResponseCode();
            if (code != 200) throw new IOException("Mojang session service returned HTTP " + code);
            try (InputStream input = connection.getInputStream();
                 Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                StringBuilder body = new StringBuilder();
                char[] buffer = new char[2048];
                int count;
                while ((count = reader.read(buffer)) != -1) {
                    if (body.length() + count > MAX_BODY_CHARS) throw new IOException("Skin response too large");
                    body.append(buffer, 0, count);
                }
                return signedTextures(playerId, body.toString());
            }
        } finally {
            connection.disconnect();
        }
    }
    public static SkinProperty signedTextures(UUID requested, String json) throws IOException {
        Objects.requireNonNull(requested, "requested");
        Objects.requireNonNull(json, "json");
        try {
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            if (!root.has("id") || !requested.toString().replace("-", "")
                    .equalsIgnoreCase(root.get("id").getAsString())) {
                throw new IOException("Mojang profile ID did not match the requested UUID");
            }
            JsonArray properties = root.getAsJsonArray("properties");
            if (properties == null) throw new IOException("Mojang profile has no properties");
            for (JsonElement element : properties) {
                JsonObject property = element.getAsJsonObject();
                if (property.has("name") && "textures".equals(property.get("name").getAsString())
                        && property.has("value") && property.has("signature")) {
                    return new SkinProperty(property.get("value").getAsString(),
                            property.get("signature").getAsString());
                }
            }
            throw new IOException("Mojang profile has no signed textures property");
        } catch (IllegalStateException | IllegalArgumentException | NullPointerException exception) {
            throw new IOException("Malformed Mojang session profile", exception);
        }
    }

    public static Optional<UUID> parse(String input) {
        Objects.requireNonNull(input, "input");
        String raw = input.replace("-", "");
        if (!raw.matches("[0-9A-Fa-f]{32}")) return Optional.empty();
        String formatted = raw.substring(0, 8) + "-" + raw.substring(8, 12) + "-"
                + raw.substring(12, 16) + "-" + raw.substring(16, 20) + "-" + raw.substring(20);
        return Optional.of(UUID.fromString(formatted.toLowerCase(Locale.ROOT)));
    }

}
