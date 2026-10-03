package app.meucarrinho.application.lists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.meucarrinho.application.common.port.PersistenceException;
import app.meucarrinho.application.lists.port.ShoppingListRepository;
import app.meucarrinho.domain.event.DomainEvent;
import app.meucarrinho.domain.list.ItemChanges;
import app.meucarrinho.domain.list.ItemDraft;
import app.meucarrinho.domain.list.ListDetailsChange;
import app.meucarrinho.domain.list.ListError;
import app.meucarrinho.domain.list.ListStatus;
import app.meucarrinho.domain.list.Member;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.ItemId;
import app.meucarrinho.domain.shared.ItemName;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.Money;
import app.meucarrinho.domain.shared.Quantity;
import app.meucarrinho.domain.shared.ReceiptId;
import app.meucarrinho.domain.shared.Result;
import app.meucarrinho.testfixtures.InMemoryCore;
import app.meucarrinho.testfixtures.TestIds;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
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
        ShoppingList added = core.items.add(list.id(), actor, version(list.id()), Optional.of(itemId),
                ItemDraft.of(new ItemName("banana"), Quantity.kilograms("1.5"), Optional.of(Money.brl(6_99)))).orElseThrow();
        return core.items.pick(list.id(), actor, added.version(), itemId).orElseThrow();
    }

    private long version(ListId listId) {
        return core.listRepository.findById(listId).orElseThrow().version();
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
            ShoppingList deleted = core.lists.delete(list.id(), me, list.version()).orElseThrow();
            assertThat(core.lists.get(list.id(), me).errorOrThrow()).isEqualTo(new ListError.ListNotFound(list.id()));
            core.lists.restore(list.id(), me, deleted.version()).orElseThrow();
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

            Receipt first = core.finishPurchase.finish(list.id(), me, list.version(), Optional.of(receiptId))
                    .orElseThrow();
            Receipt retry = core.finishPurchase.finish(list.id(), me, list.version(), Optional.of(receiptId))
                    .orElseThrow();

            assertThat(version(list.id())).as("the first attempt moved the version on").isGreaterThan(list.version());
            assertThat(retry).isEqualTo(first);
            assertThat(core.receiptRepository.findVisibleTo(marina, Instant.EPOCH,
                    Instant.parse("2030-01-01T00:00:00Z"))).hasSize(1);
        }

        @Test
        void a_guest_may_finish_and_the_receipt_lands_in_the_owners_history() {
            ShoppingList list = newList(marina, Optional.empty());
            ActorRef guest = ActorRef.guest(TestIds.guestId());
            core.listRepository.save(joined(list.id(), new Member(guest, new DisplayName("Tia Rosa"), core.clock.now())));
            ShoppingList picked = withPickedItem(list, guest);

            Receipt receipt = core.finishPurchase.finish(list.id(), guest, picked.version(), Optional.empty())
                    .orElseThrow();

            assertThat(core.receipts.get(receipt.id(), marina).isOk()).isTrue();
        }

        @Test
        void nothing_is_saved_when_the_receipt_cannot_be_stored() {
            ShoppingList list = withPickedItem(newList(marina, Optional.empty()), me);
            ShoppingList other = withPickedItem(newList(marina, Optional.empty()), me);
            ReceiptId taken = core.finishPurchase.finish(other.id(), me, other.version(), Optional.empty())
                    .orElseThrow().id();
            core.events.clear();

            assertThatThrownBy(() -> core.finishPurchase.finish(list.id(), me, list.version(), Optional.of(taken)))
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
            core.finishPurchase.finish(trip.id(), me, trip.version(), Optional.empty()).orElseThrow();
            ShoppingList next = newList(marina, Optional.empty());

            ShoppingList updated = core.receiptReuse.importItems(next.id(), marina, next.version(), Optional.empty())
                    .orElseThrow();

            assertThat(updated.activeItems()).extracting(item -> item.name().value()).containsExactly("banana");
        }

        @Test
        void has_nothing_to_import_before_the_first_purchase() {
            ShoppingList list = newList(marina, Optional.empty());

            assertThat(core.receiptReuse.importItems(list.id(), marina, list.version(), Optional.empty()).errorOrThrow())
                    .isEqualTo(new ListError.NoPastPurchase());
        }

        @Test
        void cannot_shop_again_from_someone_elses_receipt() {
            AccountId other = TestIds.accountId();
            ShoppingList trip = withPickedItem(newList(other, Optional.empty()), ActorRef.account(other));
            Receipt receipt = core.finishPurchase.finish(trip.id(), ActorRef.account(other), trip.version(),
                    Optional.empty()).orElseThrow();

            assertThat(core.receiptReuse.shopAgain(receipt.id(), marina, Optional.empty()).errorOrThrow())
                    .isEqualTo(new ListError.ReceiptNotFound(receipt.id()));
        }
    }

    @Nested
    class Versions {
        @Test
        void a_stale_version_is_refused_with_the_current_revision_and_nothing_changes() {
            ShoppingList list = newList(marina, Optional.empty());
            ShoppingList changed = core.items.quickAdd(list.id(), me, list.version(), Optional.empty(), "leite")
                    .orElseThrow();
            core.events.clear();

            assertThat(core.items.quickAdd(list.id(), me, list.version(), Optional.empty(), "cafe").errorOrThrow())
                    .isEqualTo(new ListError.VersionConflict(list.id(), changed.version()));
            assertThat(core.lists.get(list.id(), me).orElseThrow().activeItems()).hasSize(1);
            assertThat(version(list.id())).isEqualTo(changed.version());
            assertThat(core.events.published()).isEmpty();
        }

        @Test
        void every_list_write_checks_the_version() {
            ShoppingList list = withPickedItem(newList(marina, Optional.empty()), me);
            ListId id = list.id();
            ItemId item = list.activeItems().getFirst().id();
            long stale = list.version() - 1;
            Map<String, Supplier<Result<?, ListError>>> writes = new LinkedHashMap<>();
            writes.put("update list", () -> core.lists.update(id, me, stale,
                    ListDetailsChange.none().withName(new ListName("Feira grande"))));
            writes.put("delete list", () -> core.lists.delete(id, me, stale));
            writes.put("restore list", () -> core.lists.restore(id, me, stale));
            writes.put("add item", () -> core.items.add(id, me, stale, Optional.empty(),
                    ItemDraft.of(new ItemName("cafe"), Quantity.one(), Optional.empty())));
            writes.put("quick-add item", () -> core.items.quickAdd(id, me, stale, Optional.empty(), "cafe"));
            writes.put("edit item", () -> core.items.edit(id, me, stale, item,
                    ItemChanges.none().withName(new ItemName("maca"))));
            writes.put("pick item", () -> core.items.pick(id, me, stale, item));
            writes.put("unpick item", () -> core.items.unpick(id, me, stale, item));
            writes.put("remove item", () -> core.items.remove(id, me, stale, item));
            writes.put("restore item", () -> core.items.restore(id, me, stale, item));
            writes.put("duplicate item", () -> core.items.duplicate(id, me, stale, item, Optional.empty()));
            writes.put("import items", () -> core.receiptReuse.importItems(id, marina, stale, Optional.empty()));
            writes.put("finish", () -> core.finishPurchase.finish(id, me, stale, Optional.empty()));

            writes.forEach((name, write) -> assertThat(write.get().errorOrThrow()).as(name)
                    .isEqualTo(new ListError.VersionConflict(id, list.version())));
            assertThat(version(id)).isEqualTo(list.version());
        }

        @Test
        void a_stranger_is_told_the_list_does_not_exist_not_its_revision() {
            ShoppingList list = newList(marina, Optional.empty());
            ActorRef stranger = ActorRef.account(TestIds.accountId());

            assertThat(core.items.quickAdd(list.id(), stranger, list.version() + 7, Optional.empty(), "cafe")
                    .errorOrThrow()).isEqualTo(new ListError.ListNotFound(list.id()));
        }

        @Test
        void a_retried_add_with_the_same_item_id_returns_the_list_although_its_version_moved_on() {
            ShoppingList list = newList(marina, Optional.empty());
            ItemId itemId = TestIds.itemId();
            ShoppingList added = core.items.quickAdd(list.id(), me, list.version(), Optional.of(itemId), "leite")
                    .orElseThrow();

            ShoppingList retry = core.items.quickAdd(list.id(), me, list.version(), Optional.of(itemId), "leite")
                    .orElseThrow();

            assertThat(retry.version()).isEqualTo(added.version());
            assertThat(retry.activeItems()).extracting(item -> item.id()).containsExactly(itemId);
            assertThat(core.events.published(DomainEvent.ItemAdded.class)).hasSize(1);
        }

        @Test
        void a_retried_duplicate_with_the_same_item_id_returns_the_list_although_its_version_moved_on() {
            ShoppingList list = withPickedItem(newList(marina, Optional.empty()), me);
            ItemId source = list.activeItems().getFirst().id();
            ItemId copy = TestIds.itemId();
            ShoppingList duplicated = core.items.duplicate(list.id(), me, list.version(), source, Optional.of(copy))
                    .orElseThrow();

            ShoppingList retry = core.items.duplicate(list.id(), me, list.version(), source, Optional.of(copy))
                    .orElseThrow();

            assertThat(retry.version()).isEqualTo(duplicated.version());
            assertThat(retry.activeItems()).hasSize(2);
        }

        @Test
        void an_item_write_that_loses_the_race_to_save_is_a_version_conflict() {
            ShoppingList list = newList(marina, Optional.empty());
            ItemService items = new ItemService(racingRepository(), core.unitOfWork, core.events, core.ids,
                    core.clock);

            assertThat(items.quickAdd(list.id(), me, list.version(), Optional.empty(), "cafe").errorOrThrow())
                    .isEqualTo(new ListError.VersionConflict(list.id(), list.version() + 1));
            assertThat(core.unitOfWork.rollbacks()).isEqualTo(1);
            assertThat(core.events.published(DomainEvent.ItemAdded.class)).isEmpty();
        }

        @Test
        void a_finish_that_loses_the_race_to_save_is_a_version_conflict() {
            ShoppingList list = withPickedItem(newList(marina, Optional.empty()), me);
            FinishPurchaseService finish = new FinishPurchaseService(racingRepository(), core.receiptBook,
                    core.unitOfWork, core.events, core.ids, core.clock);

            assertThat(finish.finish(list.id(), me, list.version(), Optional.empty()).errorOrThrow())
                    .isEqualTo(new ListError.VersionConflict(list.id(), list.version() + 1));
            assertThat(core.lists.get(list.id(), me).orElseThrow().status()).isEqualTo(ListStatus.ACTIVE);
        }

        /** Another writer commits the same list between this use case's load and its save. */
        private ShoppingListRepository racingRepository() {
            return new ShoppingListRepository() {
                @Override
                public Optional<ShoppingList> findById(ListId id) {
                    return core.listRepository.findById(id);
                }

                @Override
                public ShoppingList save(ShoppingList list) {
                    core.listRepository.findById(list.id()).ifPresent(core.listRepository::save);
                    return core.listRepository.save(list);
                }

                @Override
                public List<ShoppingList> findActiveForMember(AccountId member) {
                    return core.listRepository.findActiveForMember(member);
                }
            };
        }
    }

    @Nested
    class ListingActiveLists {
        @Test
        void shows_the_callers_active_lists_newest_first_as_home_cards() {
            ShoppingList feira = withPickedItem(newList(marina, Optional.empty()), me);
            core.clock.advance(Duration.ofHours(1));
            ShoppingList shared = newList(TestIds.accountId(), Optional.empty());
            join(shared.id(), marina);
            core.clock.advance(Duration.ofHours(1));
            ShoppingList done = withPickedItem(newList(marina, Optional.empty()), me);
            core.finishPurchase.finish(done.id(), me, done.version(), Optional.empty()).orElseThrow();
            newList(TestIds.accountId(), Optional.empty());

            List<ActiveListCard> cards = core.activeLists.activeLists(marina);

            assertThat(cards).extracting(ActiveListCard::id).containsExactly(shared.id(), feira.id());
            assertThat(cards.getFirst().people()).isEqualTo(2);
            assertThat(cards.getLast()).satisfies(card -> {
                assertThat(card.name()).isEqualTo(new ListName("Feira"));
                assertThat(card.status()).isEqualTo(ListStatus.ACTIVE);
                assertThat(card.totals()).isEqualTo(feira.totals());
                assertThat(card.people()).isEqualTo(1);
            });
        }
    }

    @Test
    void quick_add_rejects_text_that_is_not_an_item() {
        ShoppingList list = newList(marina, Optional.empty());

        assertThat(core.items.quickAdd(list.id(), me, list.version(), Optional.empty(), "1,5 leite").errorOrThrow())
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
