package app.meucarrinho.application.lists;

import app.meucarrinho.application.common.port.DomainEventPublisher;
import app.meucarrinho.application.common.port.PersistenceException;
import app.meucarrinho.application.common.port.UnitOfWork;
import app.meucarrinho.application.lists.port.ShoppingListRepository;
import app.meucarrinho.domain.event.DomainEvent;
import app.meucarrinho.domain.list.ListAccessPolicy;
import app.meucarrinho.domain.list.ListError;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.Result;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
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

    Result<ShoppingList, ListError> change(ListId id, ActorRef actor, long expectedVersion,
            Function<ShoppingList, Result<?, ListError>> change) {
        return change(id, actor, expectedVersion, list -> false, change);
    }

    /**
     * Loads, checks and saves one list write (ADR 0006): strangers get {@code ListNotFound} before anything else, a
     * replay of a write that already happened returns the list as it is, and only then must the caller's version
     * match. A save that loses the race to another writer is the same {@code VersionConflict}.
     */
    Result<ShoppingList, ListError> change(ListId id, ActorRef actor, long expectedVersion,
            Predicate<ShoppingList> alreadyApplied, Function<ShoppingList, Result<?, ListError>> change) {
        return versioned(id, () -> lists.findById(id)
                .<Result<ShoppingList, ListError>>map(list -> ListAccessPolicy.require(list, actor,
                                ListAccessPolicy.Action.VIEW)
                        .flatMap(ok -> alreadyApplied.test(list)
                                ? Result.ok(list)
                                : list.requireVersion(expectedVersion)
                                        .flatMap(current -> change.apply(list))
                                        .map(ignored -> save(list))))
                .orElseGet(() -> Result.err(new ListError.ListNotFound(id))));
    }

    /** Runs a list write in a unit of work and reports a lost optimistic lock on that list as a version conflict. */
    <T> Result<T, ListError> versioned(ListId id, Supplier<Result<T, ListError>> work) {
        try {
            return unitOfWork.execute(work);
        } catch (PersistenceException.ConcurrentModification e) {
            if (!e.aggregateId().equals(id.toString())) {
                throw e;
            }
            return Result.err(new ListError.VersionConflict(id, e.currentVersion()));
        }
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
