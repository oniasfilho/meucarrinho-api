package app.meucarrinho.api.rest.lists;

import static app.meucarrinho.api.rest.ApiErrors.unwrap;

import app.meucarrinho.api.rest.ApiErrors;
import app.meucarrinho.api.rest.ApiProblem;
import app.meucarrinho.api.rest.CopyRequest;
import app.meucarrinho.api.rest.CurrentActor;
import app.meucarrinho.api.rest.Fields;
import app.meucarrinho.api.rest.Revisions;
import app.meucarrinho.api.rest.receipts.ReceiptMapper;
import app.meucarrinho.application.lists.AddItem;
import app.meucarrinho.application.lists.DuplicateItem;
import app.meucarrinho.application.lists.EditItem;
import app.meucarrinho.application.lists.ImportItemsFromReceipt;
import app.meucarrinho.application.lists.PickItem;
import app.meucarrinho.application.lists.QuickAddItem;
import app.meucarrinho.application.lists.RemoveItem;
import app.meucarrinho.application.lists.RestoreItem;
import app.meucarrinho.application.lists.UnpickItem;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ItemId;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ReceiptId;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Maps HTTP to the item input ports and back; no rules live here (spec §3). */
@RestController
class ItemsController implements ItemsApi {
    private final CurrentActor caller;
    private final AddItem addItem;
    private final QuickAddItem quickAddItem;
    private final EditItem editItem;
    private final PickItem pickItem;
    private final UnpickItem unpickItem;
    private final RemoveItem removeItem;
    private final RestoreItem restoreItem;
    private final DuplicateItem duplicateItem;
    private final ImportItemsFromReceipt importItems;

    ItemsController(CurrentActor caller, AddItem addItem, QuickAddItem quickAddItem, EditItem editItem,
            PickItem pickItem, UnpickItem unpickItem, RemoveItem removeItem, RestoreItem restoreItem,
            DuplicateItem duplicateItem, ImportItemsFromReceipt importItems) {
        this.caller = caller;
        this.addItem = addItem;
        this.quickAddItem = quickAddItem;
        this.editItem = editItem;
        this.pickItem = pickItem;
        this.unpickItem = unpickItem;
        this.removeItem = removeItem;
        this.restoreItem = restoreItem;
        this.duplicateItem = duplicateItem;
        this.importItems = importItems;
    }

    @Override
    public ResponseEntity<ListResponse> add(UUID id, @Nullable String ifMatch, AddItemRequest request) {
        ActorRef me = ActorRef.account(caller.require());
        ListId list = ListMapper.listId(id);
        long version = Revisions.expected(ifMatch);
        Optional<ItemId> itemId = ListMapper.newItemId(request.id());
        String text = request.text();
        if (text == null) {
            return ListMapper.ok(unwrap(addItem.add(list, me, version, itemId, ListMapper.draft(request)),
                    ApiErrors::of));
        }
        if (request.name() != null || request.quantity() != null || request.unitPrice() != null
                || request.note() != null) {
            throw ApiProblem.invalid("text", "TEXT_OR_FIELDS", "Send either text or the item fields, not both.");
        }
        return ListMapper.ok(unwrap(quickAddItem.quickAdd(list, me, version, itemId, text), ApiErrors::of));
    }

    @Override
    public ResponseEntity<ListResponse> edit(UUID id, UUID itemId, @Nullable String ifMatch,
            EditItemRequest request) {
        ActorRef me = ActorRef.account(caller.require());
        ListId list = ListMapper.listId(id);
        ItemId item = ListMapper.itemId(itemId);
        long version = Revisions.expected(ifMatch);
        return ListMapper.ok(unwrap(editItem.edit(list, me, version, item, ListMapper.changes(request)),
                ApiErrors::of));
    }

    @Override
    public ResponseEntity<ListResponse> setPicked(UUID id, UUID itemId, @Nullable String ifMatch,
            PickedRequest request) {
        ActorRef me = ActorRef.account(caller.require());
        ListId list = ListMapper.listId(id);
        ItemId item = ListMapper.itemId(itemId);
        long version = Revisions.expected(ifMatch);
        boolean picked = Fields.required("picked", request.picked());
        return ListMapper.ok(unwrap(picked
                ? pickItem.pick(list, me, version, item)
                : unpickItem.unpick(list, me, version, item), ApiErrors::of));
    }

    @Override
    public ResponseEntity<ListResponse> remove(UUID id, UUID itemId, @Nullable String ifMatch) {
        ActorRef me = ActorRef.account(caller.require());
        ListId list = ListMapper.listId(id);
        ItemId item = ListMapper.itemId(itemId);
        return ListMapper.ok(unwrap(removeItem.remove(list, me, Revisions.expected(ifMatch), item),
                ApiErrors::of));
    }

    @Override
    public ResponseEntity<ListResponse> restore(UUID id, UUID itemId, @Nullable String ifMatch) {
        ActorRef me = ActorRef.account(caller.require());
        ListId list = ListMapper.listId(id);
        ItemId item = ListMapper.itemId(itemId);
        return ListMapper.ok(unwrap(restoreItem.restore(list, me, Revisions.expected(ifMatch), item),
                ApiErrors::of));
    }

    @Override
    public ResponseEntity<ListResponse> duplicate(UUID id, UUID itemId, @Nullable String ifMatch,
            @Nullable CopyRequest request) {
        ActorRef me = ActorRef.account(caller.require());
        ListId list = ListMapper.listId(id);
        ItemId source = ListMapper.itemId(itemId);
        long version = Revisions.expected(ifMatch);
        Optional<ItemId> copy = ListMapper.newItemId(request == null ? null : request.id());
        return ListMapper.ok(unwrap(duplicateItem.duplicate(list, me, version, source, copy), ApiErrors::of));
    }

    @Override
    public ResponseEntity<ListResponse> importItems(UUID id, @Nullable String ifMatch,
            @Nullable ImportItemsRequest request) {
        AccountId me = caller.require();
        ListId list = ListMapper.listId(id);
        long version = Revisions.expected(ifMatch);
        Optional<ReceiptId> receipt = Optional.ofNullable(request).map(ImportItemsRequest::receiptId)
                .map(ReceiptMapper::receiptId);
        return ListMapper.ok(unwrap(importItems.importItems(list, me, version, receipt), ApiErrors::of));
    }
}
