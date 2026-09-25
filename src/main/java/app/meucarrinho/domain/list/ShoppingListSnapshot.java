package app.meucarrinho.domain.list;

import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.Budget;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.ReceiptId;
import app.meucarrinho.domain.shared.StoreName;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record ShoppingListSnapshot(
        ListId id,
        AccountId ownerId,
        ListName name,
        Optional<StoreName> store,
        Optional<Budget> budget,
        ListStatus status,
        Optional<ListStatus> statusBeforeDelete,
        List<Member> members,
        List<ItemSnapshot> items,
        long version,
        Instant createdAt,
        Instant updatedAt,
        Optional<Instant> completedAt,
        Optional<Instant> deletedAt,
        Optional<ReceiptId> receiptId) {
    public ShoppingListSnapshot {
        members = List.copyOf(members);
        items = List.copyOf(items);
    }

    public ShoppingListSnapshot withVersion(long newVersion) {
        return new ShoppingListSnapshot(id, ownerId, name, store, budget, status, statusBeforeDelete, members,
                items, newVersion, createdAt, updatedAt, completedAt, deletedAt, receiptId);
    }
}
