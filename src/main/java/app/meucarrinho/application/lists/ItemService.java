package app.meucarrinho.application.lists;

import app.meucarrinho.application.common.port.Clock;
import app.meucarrinho.application.common.port.DomainEventPublisher;
import app.meucarrinho.application.common.port.IdGenerator;
import app.meucarrinho.application.common.port.UnitOfWork;
import app.meucarrinho.application.lists.port.ShoppingListRepository;
import app.meucarrinho.domain.list.ItemChanges;
import app.meucarrinho.domain.list.ItemDraft;
import app.meucarrinho.domain.list.ListError;
import app.meucarrinho.domain.list.QuickAddParser;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ItemId;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.Result;
import java.util.Optional;
import java.util.function.Predicate;

public final class ItemService implements AddItem, QuickAddItem, EditItem, PickItem, UnpickItem, RemoveItem,
        RestoreItem, DuplicateItem {
    private final ListTransactions tx;
    private final IdGenerator ids;
    private final Clock clock;

    public ItemService(ShoppingListRepository lists, UnitOfWork unitOfWork, DomainEventPublisher events,
            IdGenerator ids, Clock clock) {
        this.tx = new ListTransactions(lists, unitOfWork, events);
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    public Result<ShoppingList, ListError> add(ListId list, ActorRef actor, long expectedVersion,
            Optional<ItemId> itemId, ItemDraft draft) {
        ItemId id = itemId.orElseGet(ids::newItemId);
        return tx.change(list, actor, expectedVersion, holds(id), l -> l.addItem(actor, id, draft, clock.now()));
    }

    @Override
    public Result<ShoppingList, ListError> quickAdd(ListId list, ActorRef actor, long expectedVersion,
            Optional<ItemId> itemId, String text) {
        return switch (QuickAddParser.parse(text)) {
            case Result.Ok<ItemDraft, QuickAddParser.Rejection>(ItemDraft draft) ->
                    add(list, actor, expectedVersion, itemId, draft);
            case Result.Err<ItemDraft, QuickAddParser.Rejection>(QuickAddParser.Rejection rejection) ->
                    Result.err(new ListError.InvalidItem(rejection.code()));
        };
    }

    @Override
    public Result<ShoppingList, ListError> edit(ListId list, ActorRef actor, long expectedVersion, ItemId itemId,
            ItemChanges changes) {
        return tx.change(list, actor, expectedVersion, l -> l.editItem(actor, itemId, changes, clock.now()));
    }

    @Override
    public Result<ShoppingList, ListError> pick(ListId list, ActorRef actor, long expectedVersion, ItemId itemId) {
        return tx.change(list, actor, expectedVersion, l -> l.pick(actor, itemId, clock.now()));
    }

    @Override
    public Result<ShoppingList, ListError> unpick(ListId list, ActorRef actor, long expectedVersion, ItemId itemId) {
        return tx.change(list, actor, expectedVersion, l -> l.unpick(actor, itemId, clock.now()));
    }

    @Override
    public Result<ShoppingList, ListError> remove(ListId list, ActorRef actor, long expectedVersion, ItemId itemId) {
        return tx.change(list, actor, expectedVersion, l -> l.removeItem(actor, itemId, clock.now()));
    }

    @Override
    public Result<ShoppingList, ListError> restore(ListId list, ActorRef actor, long expectedVersion, ItemId itemId) {
        return tx.change(list, actor, expectedVersion, l -> l.restoreItem(actor, itemId, clock.now()));
    }

    @Override
    public Result<ShoppingList, ListError> duplicate(ListId list, ActorRef actor, long expectedVersion, ItemId source,
            Optional<ItemId> newItemId) {
        ItemId id = newItemId.orElseGet(ids::newItemId);
        return tx.change(list, actor, expectedVersion, holds(id),
                l -> l.duplicateItem(actor, source, id, clock.now()));
    }

    /** A retried add or duplicate carries an item ID the list already holds (ADR 0003, ADR 0006). */
    private static Predicate<ShoppingList> holds(ItemId id) {
        return list -> list.item(id).isPresent();
    }
}
