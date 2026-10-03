package app.meucarrinho.application.lists;

import app.meucarrinho.application.common.port.Clock;
import app.meucarrinho.application.common.port.DomainEventPublisher;
import app.meucarrinho.application.common.port.IdGenerator;
import app.meucarrinho.application.common.port.UnitOfWork;
import app.meucarrinho.application.lists.port.ShoppingListRepository;
import app.meucarrinho.application.receipts.api.ReceiptBook;
import app.meucarrinho.domain.list.ListError;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ReceiptId;
import app.meucarrinho.domain.shared.Result;
import java.util.Optional;

public final class ReceiptReuseService implements ShopAgain, ImportItemsFromReceipt {
    private final ShoppingListRepository lists;
    private final ReceiptBook receipts;
    private final ListTransactions tx;
    private final IdGenerator ids;
    private final Clock clock;

    public ReceiptReuseService(ShoppingListRepository lists, ReceiptBook receipts, UnitOfWork unitOfWork,
            DomainEventPublisher events, IdGenerator ids, Clock clock) {
        this.lists = lists;
        this.receipts = receipts;
        this.tx = new ListTransactions(lists, unitOfWork, events);
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    public Result<ShoppingList, ListError> shopAgain(ReceiptId receiptId, AccountId actor, Optional<ListId> newListId) {
        return tx.inUnitOfWork(() -> {
            Optional<Receipt> receipt = receipts.findVisible(receiptId, actor);
            if (receipt.isEmpty()) {
                return Result.err(new ListError.ReceiptNotFound(receiptId));
            }
            ListId id = newListId.orElseGet(ids::newListId);
            if (lists.findById(id).isPresent()) {
                return Result.err(new ListError.ListIdTaken(id));
            }
            return Result.ok(tx.save(ShoppingList.shopAgain(receipt.get(), id, actor, ids::newItemId, clock.now())));
        });
    }

    @Override
    public Result<ShoppingList, ListError> importItems(ListId listId, AccountId actor, long expectedVersion,
            Optional<ReceiptId> receiptId) {
        ActorRef importer = ActorRef.account(actor);
        return tx.change(listId, importer, expectedVersion, list -> sourceReceipt(actor, receiptId)
                .flatMap(receipt -> list.importFrom(importer, receipt, ids::newItemId, clock.now())));
    }

    private Result<Receipt, ListError> sourceReceipt(AccountId actor, Optional<ReceiptId> receiptId) {
        Optional<Receipt> receipt = receiptId.isPresent()
                ? receipts.findVisible(receiptId.get(), actor)
                : receipts.latestVisibleTo(actor);
        return receipt.<Result<Receipt, ListError>>map(Result::ok)
                .orElseGet(() -> Result.err(receiptId.<ListError>map(ListError.ReceiptNotFound::new)
                        .orElseGet(ListError.NoPastPurchase::new)));
    }
}
