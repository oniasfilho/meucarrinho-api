package app.meucarrinho.domain.shared;

import java.util.UUID;

public record AccountId(UUID value) {
    public AccountId {
        Uuid7.require(value, "AccountId");
    }

    public static AccountId of(String value) {
        return new AccountId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
