package app.meucarrinho.bootstrap;

import app.meucarrinho.application.accounts.GetAccount;
import app.meucarrinho.application.accounts.UpsertAccount;
import app.meucarrinho.application.lists.CreateListCommand;
import app.meucarrinho.application.lists.CreateShoppingList;
import app.meucarrinho.application.lists.FinishPurchase;
import app.meucarrinho.application.lists.PickItem;
import app.meucarrinho.application.lists.QuickAddItem;
import app.meucarrinho.domain.account.Account;
import app.meucarrinho.domain.list.Item;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.Budget;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.EmailAddress;
import app.meucarrinho.domain.shared.ExternalRef;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.StoreName;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * The prototype's people and lists for trying the API by hand (spec §14), loaded through the public use cases, never
 * SQL, so it works on any persistence adapter. Each person's token subject is their lowercase name, which is what
 * {@code make token user=<name>} asks mock-oauth2 for. A person who already has an account is skipped, so restarting
 * never duplicates data.
 *
 * <p>Not seeded yet: the shared "Compras da semana" (joining needs invitations, session 3) and receipts back-dated to
 * August and September (they are dated today).
 */
final class DemoSeed implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(DemoSeed.class);

    private final UpsertAccount upsertAccount;
    private final GetAccount getAccount;
    private final CreateShoppingList createList;
    private final QuickAddItem quickAdd;
    private final PickItem pick;
    private final FinishPurchase finish;

    DemoSeed(UpsertAccount upsertAccount, GetAccount getAccount, CreateShoppingList createList, QuickAddItem quickAdd,
            PickItem pick, FinishPurchase finish) {
        this.upsertAccount = upsertAccount;
        this.getAccount = getAccount;
        this.createList = createList;
        this.quickAdd = quickAdd;
        this.pick = pick;
        this.finish = finish;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        List<String> seeded = new ArrayList<>();
        person("marina", "Marina", account -> {
            list(account, "Feira de domingo", "Feira livre", 120_00L, List.of(
                    "2 leite 5,49", "1,2 kg tomate", "0,5kg banana 6,99", "3 pão de queijo 12,90", "cafe"), 1, false);
            list(account, "Churrasco sábado", "Açougue Boi Bom", 150_00L, List.of(
                    "3 kg picanha 89,90", "2 carvão 25", "12 cerveja 4,99", "pão de alho 15,90"), 2, false);
            list(account, "Mercado do mês", "Atacadão", null, List.of(
                    "arroz R$ 24,90", "2 feijão 8,49", "sabão em pó 19.9", "6 leite 5,29", "2 un iogurte 3,99"), 5, true);
        }, seeded);
        person("jessica", "Jessica", account -> {
            list(account, "Compras da semana", "Pão de Açúcar", 300_00L, List.of(
                    "2 arroz 24,90", "café 18,90", "1 kg frango 16,90", "3x sabonete", "detergente 2,79"), 2, false);
            list(account, "Padaria", "Padaria Real", null, List.of("10 pão francês 0,90", "manteiga 12,50"), 2, true);
        }, seeded);
        person("onias", "Onias", account -> list(account, "Farmácia", null, 80_00L, List.of(
                "protetor solar 59,90", "2 escova de dente 7,50", "vitamina c 12"), 0, false), seeded);

        if (seeded.isEmpty()) {
            LOG.info("Demo seed: marina, jessica and onias already exist; nothing to do");
        } else {
            LOG.info("Demo seed: created {}. Get a token with `make token user=<name>`.", String.join(", ", seeded));
        }
    }

    private interface World {
        void build(Account account);
    }

    private void person(String subject, String name, World world, List<String> seeded) {
        ExternalRef identity = new ExternalRef(TokenActor.PROVIDER, subject);
        if (getAccount.byIdentity(identity).isOk()) {
            return;
        }
        Account account = upsertAccount.upsert(identity, new DisplayName(name),
                Optional.of(new EmailAddress(subject + "@meucarrinho.local"))).orElseThrow();
        world.build(account);
        seeded.add(subject);
    }

    /** Creates a list, quick-adds the items, picks the first {@code picked} of them and maybe finishes the trip. */
    private void list(Account owner, String name, @Nullable String store, @Nullable Long budgetCentavos,
            List<String> items, int picked, boolean finished) {
        ActorRef me = ActorRef.account(owner.id());
        ShoppingList list = createList.create(new CreateListCommand(Optional.empty(), owner.id(), new ListName(name),
                Optional.ofNullable(store).map(StoreName::new),
                Optional.ofNullable(budgetCentavos).map(Budget::brl))).orElseThrow();
        for (String text : items) {
            list = quickAdd.quickAdd(list.id(), me, list.version(), Optional.empty(), text).orElseThrow();
        }
        for (Item item : list.activeItems().subList(0, picked)) {
            list = pick.pick(list.id(), me, list.version(), item.id()).orElseThrow();
        }
        if (finished) {
            Receipt receipt = finish.finish(list.id(), me, list.version(), Optional.empty()).orElseThrow();
            LOG.debug("Demo seed: {} finished as receipt {}", name, receipt.id());
        }
    }
}
