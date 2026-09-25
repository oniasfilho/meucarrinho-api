package app.meucarrinho.domain.shared;

import java.util.UUID;

public record ListId(UUID value) {
    public ListId {
        Uuid7.require(value, "ListId");
    }

    public static ListId of(String value) {
        return new ListId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
