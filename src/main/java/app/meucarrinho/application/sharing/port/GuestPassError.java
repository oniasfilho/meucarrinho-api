package app.meucarrinho.application.sharing.port;

public sealed interface GuestPassError {
    record Unknown() implements GuestPassError {}

    record Expired() implements GuestPassError {}

    record Revoked() implements GuestPassError {}

    record StoreUnavailable(String reason) implements GuestPassError {}
}
