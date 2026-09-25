package app.meucarrinho.application.billing.port;

public sealed interface WebhookError {
    record InvalidSignature() implements WebhookError {}

    record Replayed() implements WebhookError {}

    record Malformed(String reason) implements WebhookError {}
}
