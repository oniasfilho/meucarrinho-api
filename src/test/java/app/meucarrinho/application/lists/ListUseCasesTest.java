package app.meucarrinho.application.lists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.meucarrinho.application.common.port.PersistenceException;
import app.meucarrinho.domain.list.ItemDraft;
import app.meucarrinho.domain.list.ListError;
import app.meucarrinho.domain.list.ListStatus;
import app.meucarrinho.domain.list.Member;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.ItemName;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.Money;
import app.meucarrinho.domain.shared.Quantity;
import app.meucarrinho.domain.shared.ReceiptId;
import app.meucarrinho.testfixtures.InMemoryCore;
import app.meucarrinho.testfixtures.TestIds;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ListUseCasesTest {
    private final InMemoryCore core = new InMemoryCore();
    private final AccountId marina = TestIds.accountId();
    private final ActorRef me = ActorRef.account(marina);

    private ShoppingList newList(AccountId owner, Optional<ListId> id) {
        return core.lists.create(new CreateListCommand(id, owner, new ListName("Feira"), Optional.empty(),
                Optional.empty())).orElseThrow();
    }

    private ShoppingList withPickedItem(ShoppingList list, ActorRef actor) {
        var itemId = TestIds.itemId();
        core.items.add(list.id(), actor, Optional.of(itemId),
                ItemDraft.of(new ItemName("banana"), Quantity.kilograms("1.5"), Optional.of(Money.brl(6_99)))).orElseThrow();
        return core.items.pick(list.id(), actor, itemId).orElseThrow();
    }

    @Nested
    class Creating {
        @Test
        void accepts_the_clients_id_and_is_idempotent() {
            ListId id = TestIds.listId();

            ShoppingList first = newList(marina, Optional.of(id));
            ShoppingList retry = newList(marina, Optional.of(id));

            assertThat(first.id()).isEqualTo(id);
            assertThat(retry.version()).isEqualTo(first.version());
            assertThat(core.listRepository.all()).hasSize(1);
        }

        @Test
        void refuses_an_id_that_belongs_to_someone_else() {
            ListId id = newList(TestIds.accountId(), Optional.of(TestIds.listId())).id();

            assertThat(core.lists.create(new CreateListCommand(Optional.of(id), marina, new ListName("x"),
                    Optional.empty(), Optional.empty())).errorOrThrow()).isEqualTo(new ListError.ListIdTaken(id));
        }

        @Test
        void stops_at_50_active_lists() {
            for (int i = 0; i < ListService.MAX_ACTIVE_LISTS; i++) {
                newList(marina, Optional.empty());
            }
            assertThat(core.lists.create(new CreateListCommand(Optional.empty(), marina, new ListName("51"),
                    Optional.empty(), Optional.empty())).errorOrThrow())
                    .isEqualTo(new ListError.LimitExceeded("active lists per account", 50));
        }
    }

    @Nested
    class Reading {
        @Test
        void hides_deleted_lists_and_other_peoples_lists() {
            ShoppingList list = newList(marina, Optional.empty());
            ActorRef stranger = ActorRef.account(TestIds.accountId());

            assertThat(core.lists.get(list.id(), stranger).errorOrThrow()).isEqualTo(new ListError.ListNotFound(list.id()));
            core.lists.delete(list.id(), me).orElseThrow();
            assertThat(core.lists.get(list.id(), me).errorOrThrow()).isEqualTo(new ListError.ListNotFound(list.id()));
            core.lists.restore(list.id(), me).orElseThrow();
            assertThat(core.lists.get(list.id(), me).isOk()).isTrue();
        }
    }

    @Nested
    class Duplicating {
        @Test
        void copies_live_items_unpicked_for_the_caller() {
            ShoppingList source = withPickedItem(newList(marina, Optional.empty()), me);
            AccountId jessica = TestIds.accountId();
            join(source.id(), jessica);

            ShoppingList copy = core.lists.duplicate(source.id(), jessica, Optional.empty()).orElseThrow();

            assertThat(copy.ownerId()).isEqualTo(jessica);
            assertThat(copy.activeItems()).singleElement().satisfies(item -> {
                assertThat(item.picked()).isFalse();
                assertThat(item.id()).isNotEqualTo(source.activeItems().getFirst().id());
            });
        }
    }

    @Nested
    class Finishing {
        @Test
        void a_retry_with_the_same_receipt_id_returns_the_same_receipt() {
            ShoppingList list = withPickedItem(newList(marina, Optional.empty()), me);
            ReceiptId receiptId = TestIds.receiptId();

            Receipt first = core.finishPurchase.finish(list.id(), me, Optional.of(receiptId)).orElseThrow();
            Receipt retry = core.finishPurchase.finish(list.id(), me, Optional.of(receiptId)).orElseThrow();

            assertThat(retry).isEqualTo(first);
            assertThat(core.receiptRepository.findVisibleTo(marina, Instant.EPOCH,
                    Instant.parse("2030-01-01T00:00:00Z"))).hasSize(1);
        }

        @Test
        void a_guest_may_finish_and_the_receipt_lands_in_the_owners_history() {
            ShoppingList list = newList(marina, Optional.empty());
            ActorRef guest = ActorRef.guest(TestIds.guestId());
            core.listRepository.save(joined(list.id(), new Member(guest, new DisplayName("Tia Rosa"), core.clock.now())));
            withPickedItem(list, guest);

            Receipt receipt = core.finishPurchase.finish(list.id(), guest, Optional.empty()).orElseThrow();

            assertThat(core.receipts.get(receipt.id(), marina).isOk()).isTrue();
        }

        @Test
        void nothing_is_saved_when_the_receipt_cannot_be_stored() {
            ShoppingList list = withPickedItem(newList(marina, Optional.empty()), me);
            ReceiptId taken = core.finishPurchase.finish(withPickedItem(newList(marina, Optional.empty()), me).id(), me,
                    Optional.empty()).orElseThrow().id();
            core.events.clear();

            assertThatThrownBy(() -> core.finishPurchase.finish(list.id(), me, Optional.of(taken)))
                    .isInstanceOf(PersistenceException.ConcurrentModification.class);

            assertThat(core.lists.get(list.id(), me).orElseThrow().status())
                    .as("the list save rolled back with the failed receipt").isEqualTo(ListStatus.ACTIVE);
            assertThat(core.events.published()).isEmpty();
        }
    }

    @Nested
    class ReusingReceipts {
        @Test
        void imports_the_latest_receipt_into_an_existing_list() {
            ShoppingList trip = withPickedItem(newList(marina, Optional.empty()), me);
            core.finishPurchase.finish(trip.id(), me, Optional.empty()).orElseThrow();
            ShoppingList next = newList(marina, Optional.empty());

            ShoppingList updated = core.receiptReuse.importItems(next.id(), marina, Optional.empty()).orElseThrow();

            assertThat(updated.activeItems()).extracting(item -> item.name().value()).containsExactly("banana");
        }

        @Test
        void has_nothing_to_import_before_the_first_purchase() {
            ShoppingList list = newList(marina, Optional.empty());

            assertThat(core.receiptReuse.importItems(list.id(), marina, Optional.empty()).errorOrThrow())
                    .isEqualTo(new ListError.NoPastPurchase());
        }

        @Test
        void cannot_shop_again_from_someone_elses_receipt() {
            AccountId other = TestIds.accountId();
            ShoppingList trip = withPickedItem(newList(other, Optional.empty()), ActorRef.account(other));
            Receipt receipt = core.finishPurchase.finish(trip.id(), ActorRef.account(other), Optional.empty()).orElseThrow();

            assertThat(core.receiptReuse.shopAgain(receipt.id(), marina, Optional.empty()).errorOrThrow())
                    .isEqualTo(new ListError.ReceiptNotFound(receipt.id()));
        }
    }

    @Test
    void quick_add_rejects_text_that_is_not_an_item() {
        ShoppingList list = newList(marina, Optional.empty());

        assertThat(core.items.quickAdd(list.id(), me, Optional.empty(), "1,5 leite").errorOrThrow())
                .isEqualTo(new ListError.InvalidItem("INVALID_QUANTITY"));
    }

    private void join(ListId listId, AccountId account) {
        core.listRepository.save(joined(listId, new Member(ActorRef.account(account), new DisplayName("Jessica"),
                core.clock.now())));
    }

    private ShoppingList joined(ListId listId, Member member) {
        ShoppingList list = core.listRepository.findById(listId).orElseThrow();
        list.join(member, core.clock.now()).orElseThrow();
        return list;
    }
}
