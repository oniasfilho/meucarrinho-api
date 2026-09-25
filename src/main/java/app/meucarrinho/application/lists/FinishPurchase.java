package app.meucarrinho.application.lists;

import app.meucarrinho.domain.list.ListError;
import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ReceiptId;
import app.meucarrinho.domain.shared.Result;
import java.util.Optional;

public interface FinishPurchase {
    Result<Receipt, ListError> finish(ListId list, ActorRef actor, Optional<ReceiptId> receiptId);
}
