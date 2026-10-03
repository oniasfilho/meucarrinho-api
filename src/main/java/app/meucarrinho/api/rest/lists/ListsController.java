package app.meucarrinho.api.rest.lists;

import static app.meucarrinho.api.rest.ApiErrors.unwrap;

import app.meucarrinho.api.rest.ApiErrors;
import app.meucarrinho.api.rest.ApiProblem;
import app.meucarrinho.api.rest.CopyRequest;
import app.meucarrinho.api.rest.CurrentActor;
import app.meucarrinho.api.rest.Revisions;
import app.meucarrinho.api.rest.receipts.ReceiptMapper;
import app.meucarrinho.api.rest.receipts.ReceiptResponse;
import app.meucarrinho.application.lists.CreateShoppingList;
import app.meucarrinho.application.lists.DeleteShoppingList;
import app.meucarrinho.application.lists.DuplicateShoppingList;
import app.meucarrinho.application.lists.FinishPurchase;
import app.meucarrinho.application.lists.GetShoppingList;
import app.meucarrinho.application.lists.ListActiveLists;
import app.meucarrinho.application.lists.RestoreShoppingList;
import app.meucarrinho.application.lists.UpdateShoppingList;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ReceiptId;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Maps HTTP to the list input ports and back; no rules live here (spec §3). */
@RestController
class ListsController implements ListsApi {
    private final CurrentActor caller;
    private final ListActiveLists activeLists;
    private final CreateShoppingList createList;
    private final GetShoppingList getList;
    private final UpdateShoppingList updateList;
    private final DeleteShoppingList deleteList;
    private final RestoreShoppingList restoreList;
    private final DuplicateShoppingList duplicateList;
    private final FinishPurchase finishPurchase;

    ListsController(CurrentActor caller, ListActiveLists activeLists, CreateShoppingList createList,
            GetShoppingList getList, UpdateShoppingList updateList, DeleteShoppingList deleteList,
            RestoreShoppingList restoreList, DuplicateShoppingList duplicateList, FinishPurchase finishPurchase) {
        this.caller = caller;
        this.activeLists = activeLists;
        this.createList = createList;
        this.getList = getList;
        this.updateList = updateList;
        this.deleteList = deleteList;
        this.restoreList = restoreList;
        this.duplicateList = duplicateList;
        this.finishPurchase = finishPurchase;
    }

    @Override
    public ListCardsResponse activeLists(@Nullable String status) {
        AccountId me = caller.require();
        if (status != null && !status.equals("active")) {
            throw ApiProblem.malformed("Only status=active is supported.");
        }
        return new ListCardsResponse(activeLists.activeLists(me).stream().map(ListMapper::card).toList());
    }

    @Override
    public ResponseEntity<ListResponse> create(CreateListRequest request) {
        AccountId me = caller.require();
        return ListMapper.created(unwrap(createList.create(ListMapper.command(request, me)), ApiErrors::of));
    }

    @Override
    public ResponseEntity<ListResponse> get(UUID id) {
        ActorRef me = ActorRef.account(caller.require());
        return ListMapper.ok(unwrap(getList.get(ListMapper.listId(id), me), ApiErrors::of));
    }

    @Override
    public ResponseEntity<ListResponse> update(UUID id, @Nullable String ifMatch, UpdateListRequest request) {
        ActorRef me = ActorRef.account(caller.require());
        ListId list = ListMapper.listId(id);
        long version = Revisions.expected(ifMatch);
        return ListMapper.ok(unwrap(updateList.update(list, me, version, ListMapper.change(request)),
                ApiErrors::of));
    }

    @Override
    public ResponseEntity<ListResponse> delete(UUID id, @Nullable String ifMatch) {
        ActorRef me = ActorRef.account(caller.require());
        ListId list = ListMapper.listId(id);
        return ListMapper.ok(unwrap(deleteList.delete(list, me, Revisions.expected(ifMatch)), ApiErrors::of));
    }

    @Override
    public ResponseEntity<ListResponse> restore(UUID id, @Nullable String ifMatch) {
        ActorRef me = ActorRef.account(caller.require());
        ListId list = ListMapper.listId(id);
        return ListMapper.ok(unwrap(restoreList.restore(list, me, Revisions.expected(ifMatch)), ApiErrors::of));
    }

    @Override
    public ResponseEntity<ListResponse> duplicate(UUID id, @Nullable CopyRequest request) {
        AccountId me = caller.require();
        ListId source = ListMapper.listId(id);
        Optional<ListId> copy = Optional.ofNullable(request).map(CopyRequest::id).map(ListMapper::listId);
        return ListMapper.created(unwrap(duplicateList.duplicate(source, me, copy), ApiErrors::of));
    }

    @Override
    public ResponseEntity<ReceiptResponse> finish(UUID id, @Nullable String ifMatch, @Nullable FinishRequest request) {
        ActorRef me = ActorRef.account(caller.require());
        ListId list = ListMapper.listId(id);
        long version = Revisions.expected(ifMatch);
        Optional<ReceiptId> receiptId = Optional.ofNullable(request).map(FinishRequest::receiptId)
                .map(ReceiptMapper::receiptId);
        return ReceiptMapper.created(unwrap(finishPurchase.finish(list, me, version, receiptId), ApiErrors::of));
    }
}
