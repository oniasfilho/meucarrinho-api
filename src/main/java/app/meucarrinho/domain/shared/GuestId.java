package app.meucarrinho.domain.shared;

import java.util.UUID;

public record GuestId(UUID value) {
    public GuestId {
        Uuid7.require(value, "GuestId");
    }

    public static GuestId of(String value) {
        return new GuestId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
