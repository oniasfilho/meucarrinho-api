package app.meucarrinho.application.capabilities.port;

public record FlagKey(String value) {
    public FlagKey {
        if (value.isBlank()) {
            throw new IllegalArgumentException("Flag key cannot be blank");
        }
    }
}
