package app.meucarrinho.application.common.port;

import java.util.regex.Pattern;

/** A SHA-256 digest of a request, in lowercase hex: two requests with equal fingerprints are the same request. */
public record RequestFingerprint(String value) {
    private static final Pattern SHA_256_HEX = Pattern.compile("[0-9a-f]{64}");

    public RequestFingerprint {
        if (!SHA_256_HEX.matcher(value).matches()) {
            throw new IllegalArgumentException("A request fingerprint is 64 lowercase hex characters");
        }
    }
}
