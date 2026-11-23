package dev.fyre.disguise.skin;

import dev.fyre.disguise.api.SkinProperty;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
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
                return MojangProfileParser.signedTextures(playerId, body.toString());
            }
        } finally {
            connection.disconnect();
        }
    }
}
