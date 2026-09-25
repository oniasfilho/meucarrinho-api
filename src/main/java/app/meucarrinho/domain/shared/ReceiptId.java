package app.meucarrinho.domain.shared;

import java.util.UUID;

public record ReceiptId(UUID value) {
    public ReceiptId {
        Uuid7.require(value, "ReceiptId");
    }

    public static ReceiptId of(String value) {
        return new ReceiptId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
