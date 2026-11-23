package dev.fyre.disguise.api;

import java.util.Base64;
import java.util.Objects;

public final class SkinProperty {
    private static final int MAX_ENCODED_LENGTH = 32768;
    private final String value;
    private final String signature;

    public SkinProperty(String value, String signature) {
        this.value = requireBase64(value, "value");
        this.signature = signature == null ? null : requireBase64(signature, "signature");
    }

    private static String requireBase64(String input, String field) {
        Objects.requireNonNull(input, field);
        if (input.length() == 0 || input.length() > MAX_ENCODED_LENGTH
                || !input.matches("[A-Za-z0-9+/]+={0,2}")) {
            throw new IllegalArgumentException("Invalid Base64 " + field);
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(input);
            if (decoded.length == 0 || !Base64.getEncoder().encodeToString(decoded).equals(input)) {
                throw new IllegalArgumentException("Noncanonical Base64 " + field);
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid Base64 " + field, ex);
        }
        return input;
    }

    public String getValue() { return value; }
    public String getSignature() { return signature; }
    public boolean isSigned() { return signature != null; }
}
