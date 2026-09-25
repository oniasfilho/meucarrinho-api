package app.meucarrinho.application.receipts;

import app.meucarrinho.domain.shared.ReceiptId;

public sealed interface ReceiptError {
    record ReceiptNotFound(ReceiptId receiptId) implements ReceiptError {}
}
