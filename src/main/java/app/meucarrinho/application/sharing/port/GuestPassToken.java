package app.meucarrinho.application.sharing.port;

public record GuestPassToken(String value) {
    public GuestPassToken {
        if (value.length() < 32) {
            throw new IllegalArgumentException("Guest pass tokens carry at least 256 bits");
        }
    }

    @Override
    public String toString() {
        return "GuestPassToken[redacted]";
    }
}
