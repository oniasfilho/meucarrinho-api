package app.meucarrinho.application.notifications.port;

public sealed interface NotificationError {
    record Undeliverable(String reason) implements NotificationError {}

    record InvalidRecipient() implements NotificationError {}

    record ProviderUnavailable(String reason) implements NotificationError {}
}
