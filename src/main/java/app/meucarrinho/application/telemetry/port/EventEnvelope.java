package app.meucarrinho.application.telemetry.port;

import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.AppVersion;
import app.meucarrinho.domain.shared.ClientPlatform;
import app.meucarrinho.domain.shared.ListId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record EventEnvelope(
        UUID eventId,
        Instant occurredAt,
        Optional<ActorRef> actor,
        Optional<String> sessionId,
        Optional<ListId> listId,
        Optional<ClientPlatform> platform,
        Optional<AppVersion> appVersion,
        Optional<String> traceId,
        int schemaVersion) {
    public static final int SCHEMA_VERSION = 1;
}
