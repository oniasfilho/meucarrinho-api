package app.meucarrinho.application.lists;

import app.meucarrinho.application.common.port.Clock;
import app.meucarrinho.application.common.port.DomainEventPublisher;
import app.meucarrinho.application.common.port.IdGenerator;
import app.meucarrinho.application.common.port.UnitOfWork;
import app.meucarrinho.application.lists.port.ShoppingListRepository;
import app.meucarrinho.domain.list.ListAccessPolicy;
import app.meucarrinho.domain.list.ListDetailsChange;
import app.meucarrinho.domain.list.ListError;
import app.meucarrinho.domain.list.ListStatus;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.Result;
import java.util.Optional;

public final class ListService implements CreateShoppingList, GetShoppingList, UpdateShoppingList, DeleteShoppingList,
        RestoreShoppingList, DuplicateShoppingList {
    public static final int MAX_ACTIVE_LISTS = 50;

    private final ShoppingListRepository lists;
    private final ListTransactions tx;
    private final IdGenerator ids;
    private final Clock clock;

    public ListService(ShoppingListRepository lists, UnitOfWork unitOfWork, DomainEventPublisher events,
            IdGenerator ids, Clock clock) {
        this.lists = lists;
        this.tx = new ListTransactions(lists, unitOfWork, events);
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    public Result<ShoppingList, ListError> create(CreateListCommand command) {
        return tx.inUnitOfWork(() -> {
            ListId id = command.listId().orElseGet(ids::newListId);
            Optional<ShoppingList> existing = lists.findById(id);
            if (existing.isPresent()) {
                return existing.get().ownerId().equals(command.owner())
                        ? Result.ok(existing.get())
                        : Result.err(new ListError.ListIdTaken(id));
            }
            if (activeListsOwnedBy(command.owner()) >= MAX_ACTIVE_LISTS) {
                return Result.err(new ListError.LimitExceeded("active lists per account", MAX_ACTIVE_LISTS));
            }
            ShoppingList list = ShoppingList.create(id, command.owner(), command.name(), command.store(),
                    command.budget(), clock.now());
            return Result.ok(tx.save(list));
        });
    }

    @Override
    public Result<ShoppingList, ListError> get(ListId id, ActorRef actor) {
        return lists.findById(id)
                .filter(list -> list.status() != ListStatus.DELETED)
                .<Result<ShoppingList, ListError>>map(list ->
                        ListAccessPolicy.require(list, actor, ListAccessPolicy.Action.VIEW).map(ok -> list))
                .orElseGet(() -> Result.err(new ListError.ListNotFound(id)));
    }

    @Override
    public Result<ShoppingList, ListError> update(ListId id, ActorRef actor, ListDetailsChange change) {
        return tx.change(id, list -> list.updateDetails(actor, change, clock.now()));
    }

    @Override
    public Result<ShoppingList, ListError> delete(ListId id, ActorRef actor) {
        return tx.change(id, list -> list.delete(actor, clock.now()));
    }

    @Override
    public Result<ShoppingList, ListError> restore(ListId id, ActorRef actor) {
        return tx.change(id, list -> list.restore(actor, clock.now()));
    }

    @Override
    public Result<ShoppingList, ListError> duplicate(ListId sourceId, AccountId actor, Optional<ListId> newListId) {
        return tx.inUnitOfWork(() -> get(sourceId, ActorRef.account(actor)).flatMap(source -> {
            if (activeListsOwnedBy(actor) >= MAX_ACTIVE_LISTS) {
                return Result.err(new ListError.LimitExceeded("active lists per account", MAX_ACTIVE_LISTS));
            }
            ListId id = newListId.orElseGet(ids::newListId);
            if (lists.findById(id).isPresent()) {
                return Result.err(new ListError.ListIdTaken(id));
            }
            return Result.ok(tx.save(ShoppingList.duplicateOf(source, id, actor, ids::newItemId, clock.now())));
        }));
    }

    private long activeListsOwnedBy(AccountId owner) {
        return lists.findActiveForMember(owner).stream().filter(list -> list.ownerId().equals(owner)).count();
    }
}
