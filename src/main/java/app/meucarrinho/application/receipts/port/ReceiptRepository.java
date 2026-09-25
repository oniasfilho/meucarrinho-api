package app.meucarrinho.application.receipts.port;

import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ReceiptId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReceiptRepository {
    Optional<Receipt> findById(ReceiptId id);

    void save(Receipt receipt);

    List<Receipt> findVisibleTo(AccountId account, Instant from, Instant to);

    Optional<Receipt> findLatestVisibleTo(AccountId account);
}
