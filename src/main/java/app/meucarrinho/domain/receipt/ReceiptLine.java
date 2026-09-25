package app.meucarrinho.domain.receipt;

import app.meucarrinho.domain.shared.ItemName;
import app.meucarrinho.domain.shared.Money;
import app.meucarrinho.domain.shared.Quantity;
import java.util.Optional;

public record ReceiptLine(ItemName name, Quantity quantity, Optional<Money> unitPrice, Money subtotal) {
    public static ReceiptLine of(ItemName name, Quantity quantity, Optional<Money> unitPrice) {
        return new ReceiptLine(name, quantity, unitPrice, unitPrice.map(p -> p.times(quantity)).orElse(Money.ZERO));
    }
}
