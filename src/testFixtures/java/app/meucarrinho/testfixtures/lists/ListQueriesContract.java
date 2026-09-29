package app.meucarrinho.testfixtures.lists;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.application.lists.port.ListCard;
import app.meucarrinho.application.lists.port.ListQueries;
import app.meucarrinho.application.lists.port.ShoppingListRepository;
import app.meucarrinho.domain.list.ListDetailsChange;
import app.meucarrinho.domain.list.Member;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.Budget;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.StoreName;
import app.meucarrinho.testfixtures.TestIds;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class ListQueriesContract {
    public record Fixture(ShoppingListRepository lists, ListQueries queries) {}

    private static final Instant T0 = Instant.parse("2026-09-25T12:00:00Z");
    private Fixture fixture;

    protected abstract Fixture createFixture();

    @BeforeEach
    void setUp() {
        fixture = createFixture();
    }

    @Test
    void active_cards_include_lists_visible_to_member_with_derived_member_count() {
        AccountId owner = TestIds.accountId();
        AccountId member = TestIds.accountId();
        ShoppingList list = ShoppingList.create(TestIds.listId(), owner, new ListName("Mercado"),
                Optional.of(new StoreName("Feira")), Optional.of(Budget.brl(50_00)), T0);
        list.join(new Member(ActorRef.account(member), new DisplayName("Marina"), T0), T0).orElseThrow();
        fixture.lists().save(list);

        List<ListCard> cards = fixture.queries().activeCards(member);

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().id()).isEqualTo(list.id());
        assertThat(cards.getFirst().people()).isEqualTo(2);
        assertThat(cards.getFirst().totals().remainingBudget()).contains(Budget.brl(50_00).amount());
    }

    @Test
    void recent_stores_are_visible_to_member_deduplicated_by_normalized_name_and_limited() {
        AccountId member = TestIds.accountId();
        ShoppingList newer = ShoppingList.create(TestIds.listId(), member, new ListName("Feira"),
                Optional.of(new StoreName("Café Central")), Optional.empty(), T0.plusSeconds(60));
        fixture.lists().save(newer);
        ShoppingList older = ShoppingList.create(TestIds.listId(), member, new ListName("Mercado"),
                Optional.of(new StoreName("Cafe Central")), Optional.empty(), T0);
        fixture.lists().save(older);
        ShoppingList other = ShoppingList.create(TestIds.listId(), TestIds.accountId(), new ListName("Outro"),
                Optional.of(new StoreName("Não visível")), Optional.empty(), T0.plusSeconds(120));
        fixture.lists().save(other);

        assertThat(fixture.queries().recentStores(member, 5))
                .containsExactly(new StoreName("Café Central"));
        assertThat(fixture.queries().recentStores(member, 0)).isEmpty();
    }
}
