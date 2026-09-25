package app.meucarrinho.domain.shared;

public record ExternalRef(String provider, String value) {
    public ExternalRef {
        if (provider.isBlank() || value.isBlank()) {
            throw new IllegalArgumentException("External refs need a provider and a value");
        }
    }

    @Override
    public String toString() {
        return provider + ":" + value;
    }
}
