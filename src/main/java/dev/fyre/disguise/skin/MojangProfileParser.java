package dev.fyre.disguise.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.fyre.disguise.api.SkinProperty;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * Parses a Mojang sessionserver profile, rejecting a mismatched profile ID.
 * Checks property presence, not the RSA cryptographic signature.
 */
public final class MojangProfileParser {
    private MojangProfileParser() { }

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
}
