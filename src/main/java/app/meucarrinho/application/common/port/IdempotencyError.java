package app.meucarrinho.application.common.port;

public sealed interface IdempotencyError {
    record Unavailable(String reason) implements IdempotencyError {}
}
