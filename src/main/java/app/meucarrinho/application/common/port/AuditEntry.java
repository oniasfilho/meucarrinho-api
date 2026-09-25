package app.meucarrinho.application.common.port;

import app.meucarrinho.domain.shared.ActorRef;
import java.time.Instant;
import java.util.Optional;

public record AuditEntry(
        ActorRef actor,
        AuditAction action,
        String target,
        Instant at,
        Optional<String> requestId,
        Optional<String> ipHash) {}
