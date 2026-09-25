package app.meucarrinho.application.lists;

import app.meucarrinho.application.common.port.Clock;
import app.meucarrinho.application.common.port.DomainEventPublisher;
import app.meucarrinho.application.common.port.IdGenerator;
import app.meucarrinho.application.common.port.UnitOfWork;
import app.meucarrinho.application.lists.port.ShoppingListRepository;
import app.meucarrinho.application.receipts.api.ReceiptBook;
import app.meucarrinho.domain.event.DomainEvent;
import app.meucarrinho.domain.list.ListAccessPolicy;
import app.meucarrinho.domain.list.ListError;
import app.meucarrinho.domain.list.ListStatus;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ReceiptId;
import app.meucarrinho.domain.shared.Result;
import java.util.List;
import java.util.Optional;

public final class FinishPurchaseService implements FinishPurchase {
    private final ShoppingListRepository lists;
    private final ReceiptBook receipts;
    private final UnitOfWork unitOfWork;
    private final DomainEventPublisher events;
    private final IdGenerator ids;
    private final Clock clock;

    public FinishPurchaseService(ShoppingListRepository lists, ReceiptBook receipts, UnitOfWork unitOfWork,
            DomainEventPublisher events, IdGenerator ids, Clock clock) {
        this.lists = lists;
        this.receipts = receipts;
        this.unitOfWork = unitOfWork;
        this.events = events;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    public Result<Receipt, ListError> finish(ListId listId, ActorRef actor, Optional<ReceiptId> receiptId) {
        return unitOfWork.execute(() -> {
            Optional<ShoppingList> found = lists.findById(listId);
            if (found.isEmpty()) {
                return Result.err(new ListError.ListNotFound(listId));
            }
            ShoppingList list = found.get();

            Optional<Receipt> alreadyFinished = retriedFinish(list, actor, receiptId);
            if (alreadyFinished.isPresent()) {
                return Result.ok(alreadyFinished.get());
            }

            ReceiptId newReceiptId = receiptId.orElseGet(ids::newReceiptId);
            return list.finish(actor, newReceiptId, clock.now()).map(receipt -> {
                List<DomainEvent> recorded = list.pullEvents();
                lists.save(list);
                receipts.record(receipt);
                events.publish(recorded);
                return receipt;
            });
        });
    }

    private Optional<Receipt> retriedFinish(ShoppingList list, ActorRef actor, Optional<ReceiptId> receiptId) {
        if (list.status() != ListStatus.COMPLETED || receiptId.isEmpty() || !list.receiptId().equals(receiptId)
                || !ListAccessPolicy.require(list, actor, ListAccessPolicy.Action.FINISH).isOk()) {
            return Optional.empty();
        }
        return receipts.findVisible(receiptId.get(), list.ownerId());
    }
}
