package app.meucarrinho.application.receipts;

import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.shared.Money;
import java.time.YearMonth;
import java.util.List;

public record ReceiptMonth(YearMonth month, Money total, List<Receipt> receipts) {
    public ReceiptMonth {
        receipts = List.copyOf(receipts);
    }
}
