package app.meucarrinho.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.application.lists.CreateListCommand;
import app.meucarrinho.application.receipts.GetReceiptHistory;
import app.meucarrinho.application.receipts.ReceiptMonth;
import app.meucarrinho.domain.account.Account;
import app.meucarrinho.domain.event.DomainEvent;
import app.meucarrinho.domain.list.Item;
import app.meucarrinho.domain.list.ListError;
import app.meucarrinho.domain.list.ListStatus;
import app.meucarrinho.domain.list.ListTotals;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.receipt.ReceiptLine;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.Budget;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.EmailAddress;
import app.meucarrinho.domain.shared.ExternalRef;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.Money;
import app.meucarrinho.domain.shared.Quantity;
import app.meucarrinho.domain.shared.StoreName;
import app.meucarrinho.testfixtures.InMemoryCore;
import java.time.Duration;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ShoppingTripScenarioTest {
    private final InMemoryCore core = new InMemoryCore();

    @Test
    void marina_plans_shops_goes_over_budget_finishes_and_shops_again() {
        Account marina = core.accounts.upsert(new ExternalRef("auth0", "google-oauth2|marina"),
                new DisplayName("Marina Souza"), Optional.of(new EmailAddress("marina@example.com"))).orElseThrow();
        ActorRef me = ActorRef.account(marina.id());

        ShoppingList list = core.lists.create(new CreateListCommand(Optional.empty(), marina.id(),
                new ListName("Compras da semana"), Optional.of(new StoreName("Mercado Dia")),
                Optional.of(Budget.brl(50_00)))).orElseThrow();

        for (String text : List.of("2 leite 5,49", "cafe 18,90", "arroz R$ 24,90", "1,2 kg tomate 8,90", "pao")) {
            list = core.items.quickAdd(list.id(), me, Optional.empty(), text).orElseThrow();
        }
        assertThat(list.activeItems()).extracting(item -> item.name().value())
                .containsExactly("leite", "cafe", "arroz", "tomate", "pao");
        Item leite = item(list, "leite");
        assertThat(leite.quantity()).isEqualTo(Quantity.units(2));
        assertThat(leite.subtotal()).contains(Money.brl(10_98));
        assertThat(item(list, "tomate").subtotal()).contains(Money.brl(10_68));
        assertThat(list.totals().estimatedTotal()).isEqualTo(Money.brl(10_98 + 18_90 + 24_90 + 10_68));
        assertThat(list.totals().unpricedCount()).isEqualTo(1);

        core.clock.advance(Duration.ofHours(1));
        list = core.items.pick(list.id(), me, item(list, "leite").id()).orElseThrow();
        list = core.items.pick(list.id(), me, item(list, "cafe").id()).orElseThrow();
        assertThat(list.totals().remainingBudget()).contains(Money.brl(50_00 - 29_88));
        assertThat(core.events.published(DomainEvent.BudgetExceeded.class)).isEmpty();

        list = core.items.pick(list.id(), me, item(list, "arroz").id()).orElseThrow();
        ListTotals totals = list.totals();
        assertThat(totals.pickedTotal()).isEqualTo(Money.brl(54_78));
        assertThat(totals.overBudget()).isTrue();
        assertThat(totals.remainingBudget()).contains(Money.brl(-4_78));
        assertThat(totals.pendingCount()).isEqualTo(2);
        assertThat(core.events.published(DomainEvent.BudgetExceeded.class)).singleElement()
                .satisfies(e -> assertThat(e.pickedTotal()).isEqualTo(Money.brl(54_78)));

        core.clock.advance(Duration.ofMinutes(20));
        Receipt receipt = core.finishPurchase.finish(list.id(), me, Optional.empty()).orElseThrow();
        assertThat(receipt.lines()).extracting(line -> line.name().value()).containsExactly("leite", "cafe", "arroz");
        assertThat(receipt.total()).isEqualTo(Money.brl(54_78));
        assertThat(receipt.budgetDelta()).contains(Money.brl(-4_78));
        assertThat(core.events.published(DomainEvent.PurchaseFinished.class)).singleElement()
                .satisfies(e -> assertThat(e.pendingDropped()).isEqualTo(2));

        ShoppingList finished = core.lists.get(list.id(), me).orElseThrow();
        assertThat(finished.status()).isEqualTo(ListStatus.COMPLETED);
        assertThat(finished.activeItems()).hasSize(5);
        assertThat(core.items.pick(list.id(), me, item(finished, "tomate").id()).errorOrThrow())
                .isEqualTo(new ListError.ListNotActive(list.id(), ListStatus.COMPLETED));

        assertThat(core.receipts.get(receipt.id(), marina.id()).orElseThrow()).isEqualTo(receipt);
        YearMonth september = YearMonth.of(2026, 9);
        List<ReceiptMonth> history = core.receipts.history(marina.id(), september, september,
                GetReceiptHistory.DEFAULT_ZONE);
        assertThat(history).singleElement().satisfies(month -> {
            assertThat(month.total()).isEqualTo(Money.brl(54_78));
            assertThat(month.receipts()).containsExactly(receipt);
        });

        core.clock.advance(Duration.ofDays(7));
        ShoppingList again = core.receiptReuse.shopAgain(receipt.id(), marina.id(), Optional.empty()).orElseThrow();
        assertThat(again.status()).isEqualTo(ListStatus.ACTIVE);
        assertThat(again.name()).isEqualTo(new ListName("Compras da semana"));
        assertThat(again.budget()).contains(Budget.brl(50_00));
        assertThat(again.activeItems()).extracting(item -> item.name().value()).containsExactly("leite", "cafe", "arroz");
        assertThat(again.activeItems()).noneMatch(Item::picked);
        assertThat(again.totals().estimatedTotal()).isEqualTo(receipt.lines().stream()
                .map(ReceiptLine::subtotal).reduce(Money.ZERO, Money::plus));
        assertThat(core.listQueries.activeCards(marina.id())).extracting(card -> card.id()).containsExactly(again.id());

        assertThat(core.unitOfWork.rollbacks()).isZero();
        assertThat(core.events.published(DomainEvent.ListCreated.class)).hasSize(1);
        assertThat(core.events.published(DomainEvent.ItemAdded.class)).hasSize(5);
        assertThat(core.events.published(DomainEvent.ItemPicked.class)).hasSize(3);
        assertThat(core.events.published(DomainEvent.ListShoppedAgain.class)).hasSize(1);
        assertThat(core.analytics.events()).as("list events are app-owned (spec §9)").isEmpty();
        assertThat(core.email.sent()).isEmpty();
        assertThat(core.push.sent()).isEmpty();
    }

    private static Item item(ShoppingList list, String name) {
        return list.activeItems().stream().filter(i -> i.name().value().equals(name)).findFirst().orElseThrow();
    }
}
