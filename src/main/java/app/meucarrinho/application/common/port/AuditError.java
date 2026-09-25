package app.meucarrinho.application.common.port;

public sealed interface AuditError {
    record Unavailable(String reason) implements AuditError {}
}
