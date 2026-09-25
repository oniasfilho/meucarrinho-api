package app.meucarrinho.application.lists;

import app.meucarrinho.application.common.port.DomainEventPublisher;
import app.meucarrinho.application.common.port.UnitOfWork;
import app.meucarrinho.application.lists.port.ShoppingListRepository;
import app.meucarrinho.domain.event.DomainEvent;
import app.meucarrinho.domain.list.ListError;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.Result;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

final class ListTransactions {
    private final ShoppingListRepository lists;
    private final UnitOfWork unitOfWork;
    private final DomainEventPublisher events;

    ListTransactions(ShoppingListRepository lists, UnitOfWork unitOfWork, DomainEventPublisher events) {
        this.lists = lists;
        this.unitOfWork = unitOfWork;
        this.events = events;
    }

    Result<ShoppingList, ListError> change(ListId id, Function<ShoppingList, Result<?, ListError>> change) {
        return unitOfWork.execute(() -> lists.findById(id)
                .<Result<ShoppingList, ListError>>map(list -> change.apply(list).map(ignored -> save(list)))
                .orElseGet(() -> Result.err(new ListError.ListNotFound(id))));
    }

    ShoppingList save(ShoppingList list) {
        List<DomainEvent> recorded = list.pullEvents();
        ShoppingList saved = lists.save(list);
        events.publish(recorded);
        return saved;
    }

    <T> T inUnitOfWork(Supplier<T> work) {
        return unitOfWork.execute(work);
    }
}
