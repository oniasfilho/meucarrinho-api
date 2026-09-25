package app.meucarrinho.application.receipts.api;

import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ReceiptId;
import java.util.Optional;

public interface ReceiptBook {
    void record(Receipt receipt);

    Optional<Receipt> findVisible(ReceiptId id, AccountId viewer);

    Optional<Receipt> latestVisibleTo(AccountId viewer);
}
