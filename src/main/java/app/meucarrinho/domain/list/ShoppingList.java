package app.meucarrinho.domain.list;

import static app.meucarrinho.domain.list.ListAccessPolicy.Action.EDIT_ITEMS;
import static app.meucarrinho.domain.list.ListAccessPolicy.Action.FINISH;
import static app.meucarrinho.domain.list.ListAccessPolicy.Action.MANAGE;

import app.meucarrinho.domain.event.DomainEvent;
import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.receipt.ReceiptLine;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.Budget;
import app.meucarrinho.domain.shared.ItemId;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.ReceiptId;
import app.meucarrinho.domain.shared.Result;
import app.meucarrinho.domain.shared.StoreName;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public final class ShoppingList {
    public static final int MAX_ITEMS = 500;

    public static final int MAX_MEMBERS = 10;
    public static final Duration RESTORE_WINDOW = Duration.ofDays(30);

    private final ListId id;
    private final AccountId ownerId;
    private ListName name;
    private Optional<StoreName> store;
    private Optional<Budget> budget;
    private ListStatus status;
    private Optional<ListStatus> statusBeforeDelete;
    private final List<Member> members;
    private final Map<ItemId, Item> items;
    private final long version;
    private final Instant createdAt;
    private Instant updatedAt;
    private Optional<Instant> completedAt;
    private Optional<Instant> deletedAt;
    private Optional<ReceiptId> receiptId;
    private final List<DomainEvent> events = new ArrayList<>();

    private ShoppingList(ShoppingListSnapshot s) {
        this.id = s.id();
        this.ownerId = s.ownerId();
        this.name = s.name();
        this.store = s.store();
        this.budget = s.budget();
        this.status = s.status();
        this.statusBeforeDelete = s.statusBeforeDelete();
        this.members = new ArrayList<>(s.members());
        this.items = new LinkedHashMap<>();
        s.items().stream()
                .sorted(Comparator.comparingInt(ItemSnapshot::position))
                .forEach(item -> items.put(item.id(), new Item(item)));
        this.version = s.version();
        this.createdAt = s.createdAt();
        this.updatedAt = s.updatedAt();
        this.completedAt = s.completedAt();
        this.deletedAt = s.deletedAt();
        this.receiptId = s.receiptId();
    }

    public static ShoppingList create(ListId id, AccountId owner, ListName name, Optional<StoreName> store,
            Optional<Budget> budget, Instant now) {
        ShoppingList list = blank(id, owner, name, store, budget, now);
        list.record(new DomainEvent.ListCreated(id, owner, budget.isPresent(), store.isPresent(),
                ActorRef.account(owner), now));
        return list;
    }

    public static ShoppingList duplicateOf(ShoppingList source, ListId id, AccountId owner,
            Supplier<ItemId> itemIds, Instant now) {
        ShoppingList copy = blank(id, owner, source.name, source.store, source.budget, now);
        ActorRef actor = ActorRef.account(owner);
        source.activeItems().forEach(item -> copy.insertItem(itemIds.get(), item.toDraft(), actor));
        copy.record(new DomainEvent.ListDuplicated(id, source.id, copy.items.size(), actor, now));
        return copy;
    }

    public static ShoppingList shopAgain(Receipt receipt, ListId id, AccountId owner, Supplier<ItemId> itemIds,
            Instant now) {
        ShoppingList list = blank(id, owner, receipt.name(), receipt.store(), receipt.budget(), now);
        ActorRef actor = ActorRef.account(owner);
        receipt.lines().forEach(line -> list.insertItem(itemIds.get(), draftOf(line), actor));
        list.record(new DomainEvent.ListShoppedAgain(id, receipt.id(), list.items.size(), actor, now));
        return list;
    }

    public static ShoppingList rehydrate(ShoppingListSnapshot snapshot) {
        return new ShoppingList(snapshot);
    }

    private static ShoppingList blank(ListId id, AccountId owner, ListName name, Optional<StoreName> store,
            Optional<Budget> budget, Instant now) {
        return new ShoppingList(new ShoppingListSnapshot(id, owner, name, store, budget, ListStatus.ACTIVE,
                Optional.empty(), List.of(), List.of(), 0, now, now, Optional.empty(), Optional.empty(),
                Optional.empty()));
    }

    public Result<@Nullable Void, ListError> updateDetails(ActorRef actor, ListDetailsChange change, Instant now) {
        return guard(actor, MANAGE).flatMap(ok -> requireActive()).map(ok -> {
            boolean wasOver = totals().overBudget();
            change.name().ifPresent(value -> name = value);
            store = change.store().applyTo(store);
            budget = change.budget().applyTo(budget);
            touch(now);
            raiseBudgetExceededIfCrossed(wasOver, actor, now);
            return null;
        });
    }

    public Result<@Nullable Void, ListError> delete(ActorRef actor, Instant now) {
        return guard(actor, MANAGE).map(ok -> {
            if (status != ListStatus.DELETED) {
                statusBeforeDelete = Optional.of(status);
                status = ListStatus.DELETED;
                deletedAt = Optional.of(now);
                touch(now);
            }
            return null;
        });
    }

    public Result<@Nullable Void, ListError> restore(ActorRef actor, Instant now) {
        return guard(actor, MANAGE).flatMap(ok -> {
            if (status != ListStatus.DELETED) {
                return Result.ok();
            }
            if (deletedAt.orElseThrow().plus(RESTORE_WINDOW).isBefore(now)) {
                return Result.err(new ListError.RestoreWindowExpired("list"));
            }
            status = statusBeforeDelete.orElse(ListStatus.ACTIVE);
            statusBeforeDelete = Optional.empty();
            deletedAt = Optional.empty();
            touch(now);
            return Result.ok();
        });
    }

    public Result<@Nullable Void, ListError> join(Member member, Instant now) {
        return requireActive().flatMap(ok -> {
            if (ListAccessPolicy.roleOf(this, member.actor()) != ListAccessPolicy.Role.NONE) {
                return Result.ok();
            }
            if (members.size() + 1 >= MAX_MEMBERS) {
                return Result.err(new ListError.LimitExceeded("members per list", MAX_MEMBERS));
            }
            members.add(member);
            touch(now);
            record(new DomainEvent.MemberJoined(id, member.actor(), member.actor(), now));
            return Result.ok();
        });
    }

    public Result<@Nullable Void, ListError> removeMember(ActorRef actor, ActorRef member, Instant now) {
        Result<@Nullable Void, ListError> allowed = actor.equals(member)
                ? guard(actor, ListAccessPolicy.Action.VIEW)
                : guard(actor, MANAGE);
        return allowed.flatMap(ok -> {
            if (ListAccessPolicy.roleOf(this, member) == ListAccessPolicy.Role.OWNER) {
                return Result.err(new ListError.OwnerOnly(id));
            }
            if (members.removeIf(m -> m.actor().equals(member))) {
                touch(now);
                record(new DomainEvent.MemberLeft(id, member, actor, now));
            }
            return Result.ok();
        });
    }

    public Result<Item, ListError> addItem(ActorRef actor, ItemId itemId, ItemDraft draft, Instant now) {
        return guard(actor, EDIT_ITEMS).flatMap(ok -> requireActive()).flatMap(ok -> {
            Item existing = items.get(itemId);
            if (existing != null) {
                return Result.ok(existing);
            }
            if (activeItems().size() >= MAX_ITEMS) {
                return Result.err(new ListError.LimitExceeded("items per list", MAX_ITEMS));
            }
            Item item = insertItem(itemId, draft, actor);
            touch(now);
            record(new DomainEvent.ItemAdded(id, itemId, actor, now));
            return Result.ok(item);
        });
    }

    public Result<Item, ListError> editItem(ActorRef actor, ItemId itemId, ItemChanges changes, Instant now) {
        return liveItem(actor, itemId).map(item -> {
            boolean wasOver = totals().overBudget();
            Set<String> changed = item.apply(changes, actor);
            if (!changed.isEmpty()) {
                touch(now);
                record(new DomainEvent.ItemEdited(id, itemId, changed, actor, now));
                raiseBudgetExceededIfCrossed(wasOver, actor, now);
            }
            return item;
        });
    }

    public Result<Item, ListError> pick(ActorRef actor, ItemId itemId, Instant now) {
        return liveItem(actor, itemId).map(item -> {
            if (!item.picked()) {
                boolean wasOver = totals().overBudget();
                item.pick(actor);
                touch(now);
                record(new DomainEvent.ItemPicked(id, itemId, actor, now));
                raiseBudgetExceededIfCrossed(wasOver, actor, now);
            }
            return item;
        });
    }

    public Result<Item, ListError> unpick(ActorRef actor, ItemId itemId, Instant now) {
        return liveItem(actor, itemId).map(item -> {
            if (item.picked()) {
                item.unpick();
                touch(now);
                record(new DomainEvent.ItemUnpicked(id, itemId, actor, now));
            }
            return item;
        });
    }

    public Result<Item, ListError> removeItem(ActorRef actor, ItemId itemId, Instant now) {
        return anyItem(actor, itemId).map(item -> {
            if (!item.isRemoved()) {
                item.remove(now);
                touch(now);
                record(new DomainEvent.ItemRemoved(id, itemId, actor, now));
            }
            return item;
        });
    }

    public Result<Item, ListError> restoreItem(ActorRef actor, ItemId itemId, Instant now) {
        return anyItem(actor, itemId).flatMap(item -> {
            if (!item.isRemoved()) {
                return Result.ok(item);
            }
            if (item.removedAt().orElseThrow().plus(RESTORE_WINDOW).isBefore(now)) {
                return Result.err(new ListError.RestoreWindowExpired("item"));
            }
            if (activeItems().size() >= MAX_ITEMS) {
                return Result.err(new ListError.LimitExceeded("items per list", MAX_ITEMS));
            }
            boolean wasOver = totals().overBudget();
            item.restore();
            touch(now);
            record(new DomainEvent.ItemRestored(id, itemId, actor, now));
            raiseBudgetExceededIfCrossed(wasOver, actor, now);
            return Result.ok(item);
        });
    }

    public Result<Item, ListError> duplicateItem(ActorRef actor, ItemId sourceId, ItemId newId, Instant now) {
        return liveItem(actor, sourceId).flatMap(source -> addItem(actor, newId, source.toDraft(), now));
    }

    public Result<List<Item>, ListError> importFrom(ActorRef actor, Receipt receipt, Supplier<ItemId> itemIds,
            Instant now) {
        return guard(actor, EDIT_ITEMS).flatMap(ok -> requireActive()).flatMap(ok -> {
            if (activeItems().size() + receipt.lines().size() > MAX_ITEMS) {
                return Result.err(new ListError.LimitExceeded("items per list", MAX_ITEMS));
            }
            List<Item> added = new ArrayList<>();
            for (ReceiptLine line : receipt.lines()) {
                ItemId itemId = itemIds.get();
                added.add(insertItem(itemId, draftOf(line), actor));
                record(new DomainEvent.ItemAdded(id, itemId, actor, now));
            }
            touch(now);
            return Result.ok(added);
        });
    }

    public Result<Receipt, ListError> finish(ActorRef actor, ReceiptId newReceiptId, Instant now) {
        return guard(actor, FINISH).flatMap(ok -> requireActive()).flatMap(ok -> {
            List<Item> picked = activeItems().stream().filter(Item::picked).toList();
            if (picked.isEmpty()) {
                return Result.err(new ListError.NothingPicked(id));
            }
            List<ReceiptLine> lines = picked.stream()
                    .map(item -> ReceiptLine.of(item.name(), item.quantity(), item.unitPrice()))
                    .toList();
            List<ActorRef> participants = new ArrayList<>();
            participants.add(ActorRef.account(ownerId));
            members.forEach(member -> participants.add(member.actor()));
            Receipt receipt = Receipt.of(newReceiptId, id, name, store, now, actor, participants, lines, budget);
            int pendingDropped = activeItems().size() - picked.size();

            status = ListStatus.COMPLETED;
            completedAt = Optional.of(now);
            receiptId = Optional.of(newReceiptId);
            touch(now);
            record(new DomainEvent.PurchaseFinished(id, newReceiptId, receipt.total(), receipt.budgetDelta(),
                    lines.size(), pendingDropped, participants.size(), actor, now));
            return Result.ok(receipt);
        });
    }

    public ListTotals totals() {
        return ListTotals.of(items.values(), budget);
    }

    public ListId id() {
        return id;
    }

    public AccountId ownerId() {
        return ownerId;
    }

    public ListName name() {
        return name;
    }

    public Optional<StoreName> store() {
        return store;
    }

    public Optional<Budget> budget() {
        return budget;
    }

    public ListStatus status() {
        return status;
    }

    public List<Member> members() {
        return Collections.unmodifiableList(members);
    }

    public List<Item> activeItems() {
        return items.values().stream().filter(item -> !item.isRemoved()).toList();
    }

    public List<Item> allItems() {
        return List.copyOf(items.values());
    }

    public Optional<Item> item(ItemId itemId) {
        return Optional.ofNullable(items.get(itemId));
    }

    public long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Optional<Instant> completedAt() {
        return completedAt;
    }

    public Optional<Instant> deletedAt() {
        return deletedAt;
    }

    public Optional<ReceiptId> receiptId() {
        return receiptId;
    }

    public ShoppingListSnapshot snapshot() {
        return new ShoppingListSnapshot(id, ownerId, name, store, budget, status, statusBeforeDelete,
                members, items.values().stream().map(Item::snapshot).toList(), version, createdAt, updatedAt,
                completedAt, deletedAt, receiptId);
    }

    public List<DomainEvent> pullEvents() {
        List<DomainEvent> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }

    private Result<@Nullable Void, ListError> guard(ActorRef actor, ListAccessPolicy.Action action) {
        return ListAccessPolicy.require(this, actor, action);
    }

    private Result<@Nullable Void, ListError> requireActive() {
        return status == ListStatus.ACTIVE ? Result.ok() : Result.err(new ListError.ListNotActive(id, status));
    }

    private Result<Item, ListError> liveItem(ActorRef actor, ItemId itemId) {
        return anyItem(actor, itemId).flatMap(item -> item.isRemoved()
                ? Result.err(new ListError.ItemDeleted(itemId))
                : Result.ok(item));
    }

    private Result<Item, ListError> anyItem(ActorRef actor, ItemId itemId) {
        return guard(actor, EDIT_ITEMS).flatMap(ok -> requireActive()).flatMap(ok -> {
            Item item = items.get(itemId);
            return item == null ? Result.err(new ListError.ItemNotFound(itemId)) : Result.ok(item);
        });
    }

    private Item insertItem(ItemId itemId, ItemDraft draft, ActorRef actor) {
        int position = items.values().stream().mapToInt(Item::position).max().orElse(-1) + 1;
        Item item = new Item(itemId, draft, actor, position);
        items.put(itemId, item);
        return item;
    }

    private static ItemDraft draftOf(ReceiptLine line) {
        return ItemDraft.of(line.name(), line.quantity(), line.unitPrice());
    }

    private void raiseBudgetExceededIfCrossed(boolean wasOver, ActorRef actor, Instant now) {
        ListTotals totals = totals();
        if (!wasOver && totals.overBudget()) {
            record(new DomainEvent.BudgetExceeded(id, totals.pickedTotal(), budget.orElseThrow().amount(),
                    totals.pendingCount(), actor, now));
        }
    }

    private void touch(Instant now) {
        updatedAt = now;
    }

    private void record(DomainEvent event) {
        events.add(event);
    }
}
