package app.meucarrinho.domain.shared;

import java.util.UUID;

public record ItemId(UUID value) {
    public ItemId {
        Uuid7.require(value, "ItemId");
    }

    public static ItemId of(String value) {
        return new ItemId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
