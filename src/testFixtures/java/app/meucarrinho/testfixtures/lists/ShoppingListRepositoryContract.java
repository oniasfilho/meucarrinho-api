package app.meucarrinho.testfixtures.lists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.meucarrinho.application.common.port.PersistenceException;
import app.meucarrinho.application.lists.port.ShoppingListRepository;
import app.meucarrinho.domain.list.ItemDraft;
import app.meucarrinho.domain.list.ListStatus;
import app.meucarrinho.domain.list.Member;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.Budget;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.ItemName;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.Money;
import app.meucarrinho.domain.shared.Quantity;
import app.meucarrinho.domain.shared.StoreName;
import app.meucarrinho.testfixtures.TestIds;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class ShoppingListRepositoryContract {
    private static final Instant T0 = Instant.parse("2026-09-25T12:00:00Z");

    private ShoppingListRepository repository;
    private final AccountId marina = TestIds.accountId();

    protected abstract ShoppingListRepository createRepository();

    @BeforeEach
    void setUp() {
        repository = createRepository();
    }

    private ShoppingList newList(AccountId owner, Instant at) {
        ShoppingList list = ShoppingList.create(TestIds.listId(), owner, new ListName("Compras da semana"),
                Optional.of(new StoreName("Mercado")), Optional.of(Budget.brl(150_00)), at);
        list.addItem(ActorRef.account(owner), TestIds.itemId(),
                ItemDraft.of(new ItemName("Café"), Quantity.one(), Optional.of(Money.brl(18_90))), at).orElseThrow();
        list.addItem(ActorRef.account(owner), TestIds.itemId(),
                ItemDraft.of(new ItemName("tomate"), Quantity.kilograms("1.2"), Optional.empty()), at).orElseThrow();
        return list;
    }

    @Test
    void saves_and_loads_the_whole_aggregate() {
        ShoppingList list = newList(marina, T0);

        ShoppingList saved = repository.save(list);

        assertThat(saved.version()).isEqualTo(1);
        ShoppingList loaded = repository.findById(list.id()).orElseThrow();
        assertThat(loaded.snapshot()).isEqualTo(list.snapshot().withVersion(1));
        assertThat(loaded.totals()).isEqualTo(list.totals());
    }

    @Test
    void does_not_persist_events() {
        repository.save(newList(marina, T0));

        assertThat(repository.findById(repository.findActiveForMember(marina).getFirst().id()).orElseThrow().pullEvents())
                .isEmpty();
    }

    @Test
    void increments_the_version_on_every_save() {
        ShoppingList v1 = repository.save(newList(marina, T0));
        v1.pick(ActorRef.account(marina), v1.activeItems().getFirst().id(), T0).orElseThrow();

        ShoppingList v2 = repository.save(v1);

        assertThat(v2.version()).isEqualTo(2);
        assertThat(repository.findById(v1.id()).orElseThrow().totals().pickedCount()).isEqualTo(1);
    }

    @Test
    void rejects_a_save_from_a_stale_copy() {
        ShoppingList saved = repository.save(newList(marina, T0));
        ShoppingList first = repository.findById(saved.id()).orElseThrow();
        ShoppingList second = repository.findById(saved.id()).orElseThrow();
        repository.save(first);

        assertThatThrownBy(() -> repository.save(second))
                .isInstanceOfSatisfying(PersistenceException.ConcurrentModification.class,
                        e -> assertThat(e.currentVersion()).isEqualTo(2));
    }

    @Test
    void rejects_a_new_list_whose_id_is_taken() {
        ShoppingList saved = repository.save(newList(marina, T0));
        ShoppingList impostor = ShoppingList.create(saved.id(), TestIds.accountId(), new ListName("Outra"),
                Optional.empty(), Optional.empty(), T0);

        assertThatThrownBy(() -> repository.save(impostor))
                .isInstanceOf(PersistenceException.ConcurrentModification.class);
    }

    @Test
    void does_not_see_changes_that_were_never_saved() {
        ShoppingList saved = repository.save(newList(marina, T0));
        saved.pick(ActorRef.account(marina), saved.activeItems().getFirst().id(), T0).orElseThrow();

        assertThat(repository.findById(saved.id()).orElseThrow().totals().pickedCount()).isZero();
    }

    @Test
    void finds_nothing_for_an_unknown_id() {
        assertThat(repository.findById(TestIds.listId())).isEmpty();
    }

    @Test
    void still_finds_deleted_lists_by_id() {
        ShoppingList saved = repository.save(newList(marina, T0));
        saved.delete(ActorRef.account(marina), T0).orElseThrow();
        repository.save(saved);

        assertThat(repository.findById(saved.id()).orElseThrow().status()).isEqualTo(ListStatus.DELETED);
    }

    @Test
    void lists_active_lists_a_member_can_see_most_recent_first() {
        AccountId jessica = TestIds.accountId();
        ShoppingList older = repository.save(newList(marina, T0));
        ShoppingList shared = newList(jessica, T0.plusSeconds(60));
        shared.join(new Member(ActorRef.account(marina), new DisplayName("Marina"), T0.plusSeconds(60)),
                T0.plusSeconds(60)).orElseThrow();
        repository.save(shared);
        ShoppingList deleted = repository.save(newList(marina, T0.plusSeconds(120)));
        deleted.delete(ActorRef.account(marina), T0.plusSeconds(120)).orElseThrow();
        repository.save(deleted);
        repository.save(newList(TestIds.accountId(), T0.plusSeconds(180)));

        assertThat(repository.findActiveForMember(marina)).extracting(ShoppingList::id)
                .containsExactly(shared.id(), older.id());
    }
}
