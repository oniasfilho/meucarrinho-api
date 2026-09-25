package app.meucarrinho.application.accounts.port;

public sealed interface IdentityError {
    record NotFound() implements IdentityError {}

    record ProviderUnavailable(String reason) implements IdentityError {}
}
