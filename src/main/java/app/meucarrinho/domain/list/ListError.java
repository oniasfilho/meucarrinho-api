package app.meucarrinho.domain.list;

import app.meucarrinho.domain.shared.ItemId;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ReceiptId;

public sealed interface ListError {
    record ListNotFound(ListId listId) implements ListError {}

    record NotAMember(ListId listId) implements ListError {}

    record OwnerOnly(ListId listId) implements ListError {}

    record ListNotActive(ListId listId, ListStatus status) implements ListError {}

    record ItemNotFound(ItemId itemId) implements ListError {}

    record ItemDeleted(ItemId itemId) implements ListError {}

    record NothingPicked(ListId listId) implements ListError {}

    record LimitExceeded(String limit, int max) implements ListError {}

    record RestoreWindowExpired(String what) implements ListError {}

    record ListIdTaken(ListId listId) implements ListError {}

    record InvalidItem(String code) implements ListError {}

    record ReceiptNotFound(ReceiptId receiptId) implements ListError {}

    record NoPastPurchase() implements ListError {}
}
