package app.meucarrinho.application.notifications.port;

public record PushToken(String value) {
    @Override
    public String toString() {
        return "PushToken[redacted]";
    }
}
