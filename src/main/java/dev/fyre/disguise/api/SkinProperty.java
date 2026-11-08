package dev.fyre.disguise.api;

import java.util.Objects;

/**
 * Authenticated textures property data. Cryptographic signature verification
 * and skin retrieval are NOT implemented by this class.
 */
public final class SkinProperty {
    private final String value;
    private final String signature;

    public SkinProperty(String value, String signature) {
        this.value = requireBase64(value, "value");
        this.signature = signature == null ? null : requireBase64(signature, "signature");
    }

    private static String requireBase64(String input, String field) {
        Objects.requireNonNull(input, field);
        if (input.length() == 0 || input.length() > 32768 || !input.matches("[A-Za-z0-9+/]+={0,2}")) {
            throw new IllegalArgumentException("Invalid Base64 " + field);
        }
        try {
            java.util.Base64.getDecoder().decode(input);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid Base64 " + field, exception);
        }
        return input;
    }

    public String getValue() { return value; }
    public String getSignature() { return signature; }
    public boolean isSigned() { return signature != null; }
}
