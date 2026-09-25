package app.meucarrinho.application.receipts;

import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ReceiptId;
import app.meucarrinho.domain.shared.Result;

public interface GetReceipt {
    Result<Receipt, ReceiptError> get(ReceiptId id, AccountId viewer);
}
