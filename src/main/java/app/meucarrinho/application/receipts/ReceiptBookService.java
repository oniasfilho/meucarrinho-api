package app.meucarrinho.application.receipts;

import app.meucarrinho.application.receipts.api.ReceiptBook;
import app.meucarrinho.application.receipts.port.ReceiptRepository;
import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ReceiptId;
import java.util.Optional;

public final class ReceiptBookService implements ReceiptBook {
    private final ReceiptRepository receipts;

    public ReceiptBookService(ReceiptRepository receipts) {
        this.receipts = receipts;
    }

    @Override
    public void record(Receipt receipt) {
        receipts.save(receipt);
    }

    @Override
    public Optional<Receipt> findVisible(ReceiptId id, AccountId viewer) {
        return receipts.findById(id).filter(receipt -> receipt.isVisibleTo(viewer));
    }

    @Override
    public Optional<Receipt> latestVisibleTo(AccountId viewer) {
        return receipts.findLatestVisibleTo(viewer);
    }
}
