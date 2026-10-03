package app.meucarrinho.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.application.accounts.GetAccount;
import app.meucarrinho.application.accounts.UpsertAccount;
import app.meucarrinho.application.lists.ActiveListCard;
import app.meucarrinho.application.lists.CreateShoppingList;
import app.meucarrinho.application.lists.FinishPurchase;
import app.meucarrinho.application.lists.ListActiveLists;
import app.meucarrinho.application.lists.PickItem;
import app.meucarrinho.application.lists.QuickAddItem;
import app.meucarrinho.application.receipts.GetReceiptHistory;
import app.meucarrinho.application.receipts.ReceiptMonth;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ExternalRef;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;

/** The demo world loads through the use cases, once, however often the app restarts. */
@InMemoryApplicationTest
class DemoSeedTest {
    @Autowired
    private UpsertAccount upsertAccount;

    @Autowired
    private GetAccount getAccount;

    @Autowired
    private CreateShoppingList createList;

    @Autowired
    private QuickAddItem quickAdd;

    @Autowired
    private PickItem pick;

    @Autowired
    private FinishPurchase finish;

    @Autowired
    private ListActiveLists activeLists;

    @Autowired
    private GetReceiptHistory history;

    private AccountId account(String subject) {
        return getAccount.byIdentity(new ExternalRef("auth0", subject)).orElseThrow().id();
    }

    @Test
    void seeds_marina_jessica_and_onias_once() {
        DemoSeed seed = new DemoSeed(upsertAccount, getAccount, createList, quickAdd, pick, finish);
        seed.run(new DefaultApplicationArguments());
        seed.run(new DefaultApplicationArguments());

        List<ActiveListCard> marinas = activeLists.activeLists(account("marina"));
        assertThat(marinas).extracting(card -> card.name().value())
                .containsExactlyInAnyOrder("Feira de domingo", "Churrasco sábado");
        assertThat(marinas).filteredOn(card -> card.name().value().equals("Churrasco sábado"))
                .singleElement().satisfies(card -> assertThat(card.totals().overBudget()).isTrue());
        assertThat(activeLists.activeLists(account("jessica"))).hasSize(1);
        assertThat(activeLists.activeLists(account("onias"))).hasSize(1);

        YearMonth now = YearMonth.now(GetReceiptHistory.DEFAULT_ZONE);
        assertThat(history.history(account("marina"), now, now, GetReceiptHistory.DEFAULT_ZONE))
                .flatExtracting(ReceiptMonth::receipts).hasSize(1);
        assertThat(history.history(account("jessica"), now, now, GetReceiptHistory.DEFAULT_ZONE))
                .flatExtracting(ReceiptMonth::receipts).hasSize(1);
    }
}
