package app.meucarrinho.application.sharing.port;

public sealed interface RealtimeError {
    record Unavailable(String reason) implements RealtimeError {}
}
