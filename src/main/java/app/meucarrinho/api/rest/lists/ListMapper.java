package app.meucarrinho.api.rest.lists;

import app.meucarrinho.api.rest.ActorDto;
import app.meucarrinho.api.rest.Fields;
import app.meucarrinho.api.rest.MoneyDto;
import app.meucarrinho.api.rest.QuantityDto;
import app.meucarrinho.api.rest.Revisions;
import app.meucarrinho.application.lists.ActiveListCard;
import app.meucarrinho.application.lists.CreateListCommand;
import app.meucarrinho.domain.list.Item;
import app.meucarrinho.domain.list.ItemChanges;
import app.meucarrinho.domain.list.ItemDraft;
import app.meucarrinho.domain.list.ListDetailsChange;
import app.meucarrinho.domain.list.ListTotals;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.Budget;
import app.meucarrinho.domain.shared.ItemId;
import app.meucarrinho.domain.shared.ItemName;
import app.meucarrinho.domain.shared.ItemNote;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.PhotoRef;
import app.meucarrinho.domain.shared.Quantity;
import app.meucarrinho.domain.shared.StoreName;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;

/** Translates between list DTOs and the domain; the only place that knows both shapes. */
public final class ListMapper {
    private ListMapper() {}

    public static ResponseEntity<ListResponse> ok(ShoppingList list) {
        return ResponseEntity.ok().eTag(Revisions.etag(list.version())).body(response(list));
    }

    public static ResponseEntity<ListResponse> created(ShoppingList list) {
        return ResponseEntity.created(URI.create("/v1/lists/" + list.id()))
                .eTag(Revisions.etag(list.version()))
                .body(response(list));
    }

    public static ListResponse response(ShoppingList list) {
        return new ListResponse(
                list.id().value(),
                list.ownerId().value(),
                list.name().value(),
                list.store().map(StoreName::value).orElse(null),
                list.budget().map(budget -> MoneyDto.of(budget.amount())).orElse(null),
                list.status(),
                list.version(),
                list.members().stream()
                        .map(m -> new MemberResponse(ActorDto.of(m.actor()), m.displayName().value(), m.joinedAt()))
                        .toList(),
                list.activeItems().stream().map(ListMapper::item).toList(),
                totals(list.totals()),
                list.createdAt(),
                list.updatedAt(),
                list.completedAt().orElse(null),
                list.deletedAt().orElse(null),
                list.receiptId().map(id -> id.value()).orElse(null));
    }

    static ListCardResponse card(ActiveListCard card) {
        return new ListCardResponse(card.id().value(), card.name().value(),
                card.store().map(StoreName::value).orElse(null), card.status(), totals(card.totals()), card.people(),
                card.updatedAt());
    }

    static ListId listId(UUID id) {
        return Fields.id("The list ID", () -> new ListId(id));
    }

    static ItemId itemId(UUID id) {
        return Fields.id("The item ID", () -> new ItemId(id));
    }

    static Optional<ItemId> newItemId(@Nullable UUID id) {
        return Optional.ofNullable(id).map(ListMapper::itemId);
    }

    static CreateListCommand command(CreateListRequest request, AccountId owner) {
        String name = Fields.required("name", request.name());
        return new CreateListCommand(
                Optional.ofNullable(request.id()).map(ListMapper::listId),
                owner,
                Fields.parse("name", () -> new ListName(name)),
                Optional.ofNullable(request.store()).map(store -> Fields.parse("store", () -> new StoreName(store))),
                Optional.ofNullable(request.budget()).map(budget -> budget(budget)));
    }

    static ListDetailsChange change(UpdateListRequest request) {
        ListDetailsChange change = new ListDetailsChange(Optional.empty(),
                request.store().toChange(store -> Fields.parse("store", () -> new StoreName(store))),
                request.budget().toChange(ListMapper::budget));
        String name = request.name();
        return name == null ? change : change.withName(Fields.parse("name", () -> new ListName(name)));
    }

    static ItemDraft draft(AddItemRequest request) {
        String name = Fields.required("name", request.name());
        Quantity quantity = request.quantity() == null ? Quantity.one() : request.quantity().toQuantity("quantity");
        return Fields.parse("unitPrice", () -> new ItemDraft(
                Fields.parse("name", () -> new ItemName(name)),
                quantity,
                Optional.ofNullable(request.unitPrice()).map(price -> price.toMoney("unitPrice")),
                Optional.ofNullable(request.note()).map(note -> Fields.parse("note", () -> new ItemNote(note))),
                Optional.empty()));
    }

    static ItemChanges changes(EditItemRequest request) {
        ItemChanges changes = Fields.parse("unitPrice", () -> new ItemChanges(Optional.empty(), Optional.empty(),
                request.unitPrice().toChange(price -> price.toMoney("unitPrice")),
                request.note().toChange(note -> Fields.parse("note", () -> new ItemNote(note))),
                request.photoRef().toChange(ref -> Fields.parse("photoRef", () -> new PhotoRef(ref)))));
        String name = request.name();
        if (name != null) {
            changes = changes.withName(Fields.parse("name", () -> new ItemName(name)));
        }
        QuantityDto quantity = request.quantity();
        return quantity == null ? changes : changes.withQuantity(quantity.toQuantity("quantity"));
    }

    private static Budget budget(MoneyDto amount) {
        return Fields.parse("budget", () -> new Budget(amount.toMoney("budget")));
    }

    private static ItemResponse item(Item item) {
        return new ItemResponse(
                item.id().value(),
                item.name().value(),
                QuantityDto.of(item.quantity()),
                MoneyDto.ofNullable(item.unitPrice()),
                MoneyDto.ofNullable(item.subtotal()),
                item.note().map(ItemNote::value).orElse(null),
                item.photoRef().map(PhotoRef::key).orElse(null),
                item.picked(),
                item.pickedBy().map(ActorDto::of).orElse(null),
                ActorDto.of(item.lastEditedBy()),
                item.position());
    }

    private static TotalsResponse totals(ListTotals totals) {
        return new TotalsResponse(MoneyDto.of(totals.pickedTotal()), MoneyDto.of(totals.estimatedTotal()),
                MoneyDto.ofNullable(totals.remainingBudget()), totals.overBudget(), totals.pendingCount(),
                totals.pickedCount(), totals.unpricedCount());
    }
}
