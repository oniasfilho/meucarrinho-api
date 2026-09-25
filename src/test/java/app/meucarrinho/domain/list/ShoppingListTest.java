package app.meucarrinho.domain.list;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.domain.event.DomainEvent;
import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.Budget;
import app.meucarrinho.domain.shared.Change;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.ItemId;
import app.meucarrinho.domain.shared.ItemName;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.Money;
import app.meucarrinho.domain.shared.Quantity;
import app.meucarrinho.domain.shared.StoreName;
import app.meucarrinho.testfixtures.TestIds;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ShoppingListTest {
    private static final Instant NOW = Instant.parse("2026-09-25T14:00:00Z");

    private final AccountId marina = TestIds.accountId();
    private final ActorRef owner = ActorRef.account(marina);
    private final ActorRef jessica = ActorRef.account(TestIds.accountId());
    private final ActorRef guest = ActorRef.guest(TestIds.guestId());
    private final ActorRef stranger = ActorRef.account(TestIds.accountId());

    private ShoppingList list(Optional<Budget> budget) {
        ShoppingList list = ShoppingList.create(TestIds.listId(), marina, new ListName("Compras da semana"),
                Optional.of(new StoreName("Mercado")), budget, NOW);
        list.pullEvents();
        return list;
    }

    private ItemId add(ShoppingList list, String name, Quantity quantity, long priceCentavos) {
        ItemId id = TestIds.itemId();
        list.addItem(owner, id, ItemDraft.of(new ItemName(name), quantity, Optional.of(Money.brl(priceCentavos))), NOW)
                .orElseThrow();
        return id;
    }

    private ItemId addUnpriced(ShoppingList list, String name) {
        ItemId id = TestIds.itemId();
        list.addItem(owner, id, ItemDraft.of(new ItemName(name), Quantity.one(), Optional.empty()), NOW).orElseThrow();
        return id;
    }

    @Nested
    class Totals {
        @Test
        void are_computed_from_the_items() {
            ShoppingList list = list(Optional.of(Budget.brl(50_00)));
            ItemId leite = add(list, "leite", Quantity.units(2), 5_49);
            add(list, "tomate", Quantity.kilograms("1.2"), 8_90);
            addUnpriced(list, "cafe");
            list.pick(owner, leite, NOW).orElseThrow();

            ListTotals totals = list.totals();

            assertThat(totals.pickedTotal()).isEqualTo(Money.brl(10_98));
            assertThat(totals.estimatedTotal()).isEqualTo(Money.brl(10_98 + 10_68));
            assertThat(totals.remainingBudget()).contains(Money.brl(50_00 - 10_98));
            assertThat(totals.overBudget()).isFalse();
            assertThat(totals.pendingCount()).isEqualTo(2);
            assertThat(totals.pickedCount()).isEqualTo(1);
            assertThat(totals.unpricedCount()).isEqualTo(1);
        }

        @Test
        void follow_edits_and_removals_without_being_stored() {
            ShoppingList list = list(Optional.empty());
            ItemId leite = add(list, "leite", Quantity.units(2), 5_49);
            list.editItem(owner, leite, ItemChanges.none().withUnitPrice(Change.set(Money.brl(6_00))), NOW).orElseThrow();
            assertThat(list.totals().estimatedTotal()).isEqualTo(Money.brl(12_00));

            list.removeItem(owner, leite, NOW).orElseThrow();
            assertThat(list.totals().estimatedTotal()).isEqualTo(Money.ZERO);
            assertThat(list.totals().remainingBudget()).isEmpty();
        }
    }

    @Nested
    class OverBudget {
        @Test
        void is_raised_once_when_the_picked_total_crosses_the_budget() {
            ShoppingList list = list(Optional.of(Budget.brl(20_00)));
            ItemId cafe = add(list, "cafe", Quantity.one(), 18_90);
            ItemId leite = add(list, "leite", Quantity.units(2), 5_49);
            ItemId pao = add(list, "pao", Quantity.one(), 7_00);
            list.pullEvents();

            list.pick(owner, cafe, NOW).orElseThrow();
            assertThat(list.pullEvents()).noneMatch(DomainEvent.BudgetExceeded.class::isInstance);

            list.pick(jessicaAsMember(list), leite, NOW).orElseThrow();
            assertThat(list.pullEvents()).filteredOn(DomainEvent.BudgetExceeded.class::isInstance).singleElement()
                    .satisfies(e -> {
                        var exceeded = (DomainEvent.BudgetExceeded) e;
                        assertThat(exceeded.pickedTotal()).isEqualTo(Money.brl(29_88));
                        assertThat(exceeded.pendingItems()).isEqualTo(1);
                    });
            assertThat(list.totals().overBudget()).isTrue();
            assertThat(list.totals().remainingBudget()).contains(Money.brl(-9_88));

            list.pick(owner, pao, NOW).orElseThrow();
            assertThat(list.pullEvents()).noneMatch(DomainEvent.BudgetExceeded.class::isInstance);
        }

        @Test
        void is_raised_when_the_owner_lowers_the_budget_below_the_cart() {
            ShoppingList list = list(Optional.of(Budget.brl(100_00)));
            list.pick(owner, add(list, "carne", Quantity.kilograms("1.5"), 45_00), NOW).orElseThrow();
            list.pullEvents();

            list.updateDetails(owner, ListDetailsChange.none().withBudget(Change.set(Budget.brl(50_00))), NOW)
                    .orElseThrow();

            assertThat(list.pullEvents()).hasAtLeastOneElementOfType(DomainEvent.BudgetExceeded.class);
        }

        private ActorRef jessicaAsMember(ShoppingList list) {
            list.join(new Member(jessica, new DisplayName("Jessica"), NOW), NOW).orElseThrow();
            return jessica;
        }
    }

    @Nested
    class Finishing {
        @Test
        void requires_at_least_one_picked_item() {
            ShoppingList list = list(Optional.empty());
            add(list, "leite", Quantity.one(), 5_49);

            assertThat(list.finish(owner, TestIds.receiptId(), NOW).errorOrThrow())
                    .isEqualTo(new ListError.NothingPicked(list.id()));
            assertThat(list.status()).isEqualTo(ListStatus.ACTIVE);
        }

        @Test
        void keeps_only_picked_items_on_the_receipt_and_all_items_on_the_list() {
            ShoppingList list = list(Optional.of(Budget.brl(20_00)));
            ItemId leite = add(list, "leite", Quantity.units(2), 5_49);
            ItemId cafe = add(list, "cafe", Quantity.one(), 18_90);
            add(list, "tomate", Quantity.kilograms("1.2"), 8_90);
            list.pick(owner, leite, NOW).orElseThrow();
            list.pick(owner, cafe, NOW).orElseThrow();
            list.pullEvents();

            Receipt receipt = list.finish(owner, TestIds.receiptId(), NOW).orElseThrow();

            assertThat(receipt.lines()).extracting(line -> line.name().value()).containsExactly("leite", "cafe");
            assertThat(receipt.total()).isEqualTo(Money.brl(29_88));
            assertThat(receipt.budgetDelta()).contains(Money.brl(-9_88));
            assertThat(receipt.overBudget()).isTrue();
            assertThat(list.status()).isEqualTo(ListStatus.COMPLETED);
            assertThat(list.receiptId()).contains(receipt.id());
            assertThat(list.activeItems()).hasSize(3);
            assertThat(list.pullEvents()).singleElement().isInstanceOfSatisfying(DomainEvent.PurchaseFinished.class,
                    e -> assertThat(e.pendingDropped()).isEqualTo(1));
        }

        @Test
        void a_guest_member_may_finish() {
            ShoppingList list = list(Optional.empty());
            list.join(new Member(guest, new DisplayName("Tia Rosa"), NOW), NOW).orElseThrow();
            list.pick(guest, add(list, "leite", Quantity.one(), 5_49), NOW).orElseThrow();

            Receipt receipt = list.finish(guest, TestIds.receiptId(), NOW).orElseThrow();

            assertThat(receipt.participants()).containsExactly(owner, guest);
            assertThat(receipt.isVisibleTo(marina)).isTrue();
        }
    }

    @Nested
    class Lifecycle {
        @Test
        void a_completed_list_is_read_only() {
            ShoppingList list = list(Optional.empty());
            ItemId leite = add(list, "leite", Quantity.one(), 5_49);
            list.pick(owner, leite, NOW).orElseThrow();
            list.finish(owner, TestIds.receiptId(), NOW).orElseThrow();

            ListError notActive = new ListError.ListNotActive(list.id(), ListStatus.COMPLETED);
            assertThat(list.addItem(owner, TestIds.itemId(),
                    ItemDraft.of(new ItemName("pao"), Quantity.one(), Optional.empty()), NOW).errorOrThrow())
                    .isEqualTo(notActive);
            assertThat(list.unpick(owner, leite, NOW).errorOrThrow()).isEqualTo(notActive);
            assertThat(list.editItem(owner, leite, ItemChanges.none().withName(new ItemName("x")), NOW).errorOrThrow())
                    .isEqualTo(notActive);
            assertThat(list.updateDetails(owner, ListDetailsChange.none().withName(new ListName("x")), NOW)
                    .errorOrThrow()).isEqualTo(notActive);
            assertThat(list.finish(owner, TestIds.receiptId(), NOW).errorOrThrow()).isEqualTo(notActive);
        }

        @Test
        void a_deleted_list_comes_back_to_its_earlier_status_within_30_days() {
            ShoppingList list = list(Optional.empty());
            list.delete(owner, NOW).orElseThrow();
            assertThat(list.status()).isEqualTo(ListStatus.DELETED);

            list.restore(owner, NOW.plus(Duration.ofDays(30))).orElseThrow();
            assertThat(list.status()).isEqualTo(ListStatus.ACTIVE);

            list.delete(owner, NOW).orElseThrow();
            assertThat(list.restore(owner, NOW.plus(Duration.ofDays(31))).errorOrThrow())
                    .isEqualTo(new ListError.RestoreWindowExpired("list"));
        }

        @Test
        void removed_items_can_be_restored_but_not_edited() {
            ShoppingList list = list(Optional.empty());
            ItemId leite = add(list, "leite", Quantity.one(), 5_49);
            list.removeItem(owner, leite, NOW).orElseThrow();

            assertThat(list.pick(owner, leite, NOW).errorOrThrow()).isEqualTo(new ListError.ItemDeleted(leite));
            list.restoreItem(owner, leite, NOW.plus(Duration.ofDays(1))).orElseThrow();
            assertThat(list.activeItems()).extracting(Item::id).containsExactly(leite);
        }
    }

    @Nested
    class Access {
        @Test
        void members_edit_items_but_only_the_owner_manages_the_list() {
            ShoppingList list = list(Optional.empty());
            list.join(new Member(guest, new DisplayName("Tia Rosa"), NOW), NOW).orElseThrow();

            assertThat(list.addItem(guest, TestIds.itemId(),
                    ItemDraft.of(new ItemName("pao"), Quantity.one(), Optional.empty()), NOW).isOk()).isTrue();
            assertThat(list.updateDetails(guest, ListDetailsChange.none().withName(new ListName("Minha")), NOW)
                    .errorOrThrow()).isEqualTo(new ListError.OwnerOnly(list.id()));
            assertThat(list.delete(guest, NOW).errorOrThrow()).isEqualTo(new ListError.OwnerOnly(list.id()));
        }

        @Test
        void strangers_are_told_the_list_does_not_exist() {
            ShoppingList list = list(Optional.empty());

            assertThat(list.addItem(stranger, TestIds.itemId(),
                    ItemDraft.of(new ItemName("pao"), Quantity.one(), Optional.empty()), NOW).errorOrThrow())
                    .isEqualTo(new ListError.ListNotFound(list.id()));
        }

        @Test
        void a_member_may_leave_and_the_owner_may_remove_anyone_else() {
            ShoppingList list = list(Optional.empty());
            list.join(new Member(jessica, new DisplayName("Jessica"), NOW), NOW).orElseThrow();
            list.join(new Member(guest, new DisplayName("Tia Rosa"), NOW), NOW).orElseThrow();

            assertThat(list.removeMember(jessica, guest, NOW).errorOrThrow()).isEqualTo(new ListError.OwnerOnly(list.id()));
            list.removeMember(jessica, jessica, NOW).orElseThrow();
            list.removeMember(owner, guest, NOW).orElseThrow();

            assertThat(list.members()).isEmpty();
            assertThat(list.removeMember(owner, owner, NOW).isOk()).isFalse();
        }
    }

    @Nested
    class Limits {
        @Test
        void a_list_holds_at_most_500_live_items() {
            ShoppingList list = list(Optional.empty());
            for (int i = 0; i < ShoppingList.MAX_ITEMS; i++) {
                addUnpriced(list, "item " + i);
            }
            assertThat(list.addItem(owner, TestIds.itemId(),
                    ItemDraft.of(new ItemName("one more"), Quantity.one(), Optional.empty()), NOW).errorOrThrow())
                    .isEqualTo(new ListError.LimitExceeded("items per list", 500));
        }

        @Test
        void a_list_has_at_most_10_people_including_the_owner() {
            ShoppingList list = list(Optional.empty());
            for (int i = 0; i < ShoppingList.MAX_MEMBERS - 1; i++) {
                list.join(new Member(ActorRef.guest(TestIds.guestId()), new DisplayName("Guest " + i), NOW), NOW)
                        .orElseThrow();
            }
            assertThat(list.join(new Member(jessica, new DisplayName("Jessica"), NOW), NOW).errorOrThrow())
                    .isEqualTo(new ListError.LimitExceeded("members per list", 10));
        }
    }

    @Nested
    class Events {
        @Test
        void an_edit_names_only_the_fields_that_changed() {
            ShoppingList list = list(Optional.empty());
            ItemId leite = add(list, "leite", Quantity.one(), 5_49);
            list.pullEvents();

            list.editItem(owner, leite, ItemChanges.none()
                    .withName(new ItemName("leite"))
                    .withUnitPrice(Change.set(Money.brl(8_90))), NOW).orElseThrow();
            list.editItem(owner, leite, ItemChanges.none().withUnitPrice(Change.set(Money.brl(8_90))), NOW)
                    .orElseThrow();

            assertThat(list.pullEvents()).singleElement().isInstanceOfSatisfying(DomainEvent.ItemEdited.class,
                    e -> assertThat(e.fields()).containsExactly("unitPrice"));
        }

        @Test
        void a_retried_add_with_the_same_id_changes_nothing() {
            ShoppingList list = list(Optional.empty());
            ItemId leite = add(list, "leite", Quantity.one(), 5_49);
            list.pullEvents();

            list.addItem(owner, leite, ItemDraft.of(new ItemName("leite"), Quantity.one(), Optional.empty()), NOW)
                    .orElseThrow();

            assertThat(list.activeItems()).hasSize(1);
            assertThat(list.pullEvents()).isEmpty();
        }
    }

    @Test
    void a_snapshot_rebuilds_the_same_list() {
        ShoppingList list = list(Optional.of(Budget.brl(30_00)));
        ItemId leite = add(list, "leite", Quantity.units(2), 5_49);
        list.pick(owner, leite, NOW).orElseThrow();
        list.removeItem(owner, add(list, "pao", Quantity.one(), 7_00), NOW).orElseThrow();

        ShoppingList copy = ShoppingList.rehydrate(list.snapshot());

        assertThat(copy.snapshot()).isEqualTo(list.snapshot());
        assertThat(copy.totals()).isEqualTo(list.totals());
    }
}
