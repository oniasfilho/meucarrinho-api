package app.meucarrinho.domain.list;

import app.meucarrinho.domain.shared.ItemName;
import app.meucarrinho.domain.shared.ItemNote;
import app.meucarrinho.domain.shared.Money;
import app.meucarrinho.domain.shared.PhotoRef;
import app.meucarrinho.domain.shared.Quantity;
import java.util.Optional;

public record ItemDraft(
        ItemName name,
        Quantity quantity,
        Optional<Money> unitPrice,
        Optional<ItemNote> note,
        Optional<PhotoRef> photoRef) {
    public ItemDraft {
        unitPrice.ifPresent(ItemDraft::requireValidPrice);
    }

    public static ItemDraft of(ItemName name, Quantity quantity, Optional<Money> unitPrice) {
        return new ItemDraft(name, quantity, unitPrice, Optional.empty(), Optional.empty());
    }

    static void requireValidPrice(Money price) {
        if (price.isNegative()) {
            throw new IllegalArgumentException("A price cannot be negative: " + price);
        }
    }
}
