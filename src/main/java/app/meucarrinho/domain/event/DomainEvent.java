package app.meucarrinho.domain.event;

import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ItemId;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.Money;
import app.meucarrinho.domain.shared.ReceiptId;
import app.meucarrinho.domain.shared.ShareCode;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public sealed interface DomainEvent {
    Instant occurredAt();

    ActorRef actor();

    record ListCreated(ListId listId, AccountId owner, boolean hasBudget, boolean hasStore,
            ActorRef actor, Instant occurredAt) implements DomainEvent {}

    record ListDuplicated(ListId listId, ListId sourceListId, int items, ActorRef actor, Instant occurredAt)
            implements DomainEvent {}

    record ListShoppedAgain(ListId listId, ReceiptId fromReceipt, int items, ActorRef actor, Instant occurredAt)
            implements DomainEvent {}

    record ItemAdded(ListId listId, ItemId itemId, ActorRef actor, Instant occurredAt) implements DomainEvent {}

    record ItemEdited(ListId listId, ItemId itemId, Set<String> fields, ActorRef actor, Instant occurredAt)
            implements DomainEvent {
        public ItemEdited {
            fields = Set.copyOf(fields);
        }
    }

    record ItemPicked(ListId listId, ItemId itemId, ActorRef actor, Instant occurredAt) implements DomainEvent {}

    record ItemUnpicked(ListId listId, ItemId itemId, ActorRef actor, Instant occurredAt) implements DomainEvent {}

    record ItemRemoved(ListId listId, ItemId itemId, ActorRef actor, Instant occurredAt) implements DomainEvent {}

    record ItemRestored(ListId listId, ItemId itemId, ActorRef actor, Instant occurredAt) implements DomainEvent {}

    record BudgetExceeded(ListId listId, Money pickedTotal, Money budget, int pendingItems,
            ActorRef actor, Instant occurredAt) implements DomainEvent {}

    record PurchaseFinished(ListId listId, ReceiptId receiptId, Money total, Optional<Money> budgetDelta,
            int items, int pendingDropped, int participants, ActorRef actor, Instant occurredAt)
            implements DomainEvent {}

    record InvitationCreated(ListId listId, ShareCode code, Instant expiresAt, ActorRef actor, Instant occurredAt)
            implements DomainEvent {}

    record MemberJoined(ListId listId, ActorRef member, ActorRef actor, Instant occurredAt) implements DomainEvent {}

    record MemberLeft(ListId listId, ActorRef member, ActorRef actor, Instant occurredAt) implements DomainEvent {}

    record GuestDataImported(AccountId account, int lists, int receipts, int ops, ActorRef actor, Instant occurredAt)
            implements DomainEvent {}

    record AccountDeleted(AccountId account, ActorRef actor, Instant occurredAt) implements DomainEvent {}

    record EntitlementChanged(AccountId account, String plan, boolean active, ActorRef actor, Instant occurredAt)
            implements DomainEvent {}
}
