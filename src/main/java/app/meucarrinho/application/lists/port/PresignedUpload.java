package app.meucarrinho.application.lists.port;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

public record PresignedUpload(URI url, Map<String, String> requiredHeaders, Instant expiresAt) {
    public PresignedUpload {
        requiredHeaders = Map.copyOf(requiredHeaders);
    }
}
