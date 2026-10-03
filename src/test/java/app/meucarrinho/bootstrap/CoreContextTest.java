package app.meucarrinho.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import app.meucarrinho.application.accounts.GetAccount;
import app.meucarrinho.application.accounts.UpsertAccount;
import app.meucarrinho.application.common.port.Clock;
import app.meucarrinho.application.common.port.IdGenerator;
import app.meucarrinho.application.lists.ActiveListCard;
import app.meucarrinho.application.lists.CreateListCommand;
import app.meucarrinho.application.lists.CreateShoppingList;
import app.meucarrinho.application.lists.FinishPurchase;
import app.meucarrinho.application.lists.ListActiveLists;
import app.meucarrinho.application.lists.PickItem;
import app.meucarrinho.application.lists.QuickAddItem;
import app.meucarrinho.application.receipts.GetReceipt;
import app.meucarrinho.domain.account.Account;
import app.meucarrinho.domain.event.DomainEvent;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.ExternalRef;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.testfixtures.InMemoryCore;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

/**
 * Boots the real application class on the testFixtures fakes: wiring and adapter selection, no Docker. Without a
 * DataSource the JDBC outbox has nothing to write to, so its auto-configuration is off too; the recording publisher
 * stands in for it.
 */
@SpringBootTest(classes = MeuCarrinhoApplication.class, properties = {
        "carrinho.adapters.persistence=memory",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.modulith.events.jdbc.JdbcEventPublicationAutoConfiguration,"
                + "org.springframework.modulith.events.config.EventPublicationAutoConfiguration"})
@Import(InMemoryPortsConfiguration.class)
class CoreContextTest {
    @Autowired
    private ApplicationContext context;

    @Autowired
    private InMemoryCore fakes;

    @Test
    void every_input_port_resolves_to_exactly_one_bean() {
        List<Class<?>> inputPorts = new ClassFileImporter().importPackages("app.meucarrinho.application").stream()
                .filter(c -> c.isInterface() && !c.getSimpleName().equals("package-info"))
                .filter(c -> c.getPackageName().matches("app\\.meucarrinho\\.application\\.[a-z]+"))
                .filter(c -> c.getEnclosingClass().isEmpty() && !c.reflect().isSealed())
                .<Class<?>>map(JavaClass::reflect)
                .toList();

        assertThat(inputPorts).contains(FinishPurchase.class, ListActiveLists.class, GetAccount.class, GetReceipt.class);
        assertThat(inputPorts).allSatisfy(port -> assertThat(context.getBeanNamesForType(port)).as(port.getSimpleName())
                .hasSize(1));
    }

    @Test
    void the_system_clock_and_uuidv7_ids_are_wired() {
        assertThat(context.getBean(Clock.class)).isInstanceOf(SystemClock.class);
        assertThat(context.getBean(IdGenerator.class)).isInstanceOf(Uuid7IdGenerator.class);
    }

    @Test
    void a_trip_runs_through_the_wired_use_cases() {
        ExternalRef subject = new ExternalRef("auth0", "google-oauth2|marina-context");
        Account marina = context.getBean(UpsertAccount.class)
                .upsert(subject, new DisplayName("Marina"), Optional.empty()).orElseThrow();
        assertThat(context.getBean(GetAccount.class).byIdentity(subject).orElseThrow().id()).isEqualTo(marina.id());
        ActorRef me = ActorRef.account(marina.id());

        ShoppingList list = context.getBean(CreateShoppingList.class).create(new CreateListCommand(Optional.empty(),
                marina.id(), new ListName("Feira"), Optional.empty(), Optional.empty())).orElseThrow();
        list = context.getBean(QuickAddItem.class)
                .quickAdd(list.id(), me, list.version(), Optional.empty(), "2 leite 5,49").orElseThrow();
        list = context.getBean(PickItem.class)
                .pick(list.id(), me, list.version(), list.activeItems().getFirst().id()).orElseThrow();
        assertThat(context.getBean(ListActiveLists.class).activeLists(marina.id()))
                .extracting(ActiveListCard::id).containsExactly(list.id());

        Receipt receipt = context.getBean(FinishPurchase.class)
                .finish(list.id(), me, list.version(), Optional.empty()).orElseThrow();

        assertThat(context.getBean(GetReceipt.class).get(receipt.id(), marina.id()).orElseThrow()).isEqualTo(receipt);
        assertThat(receipt.completedAt()).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
        assertThat(context.getBean(ListActiveLists.class).activeLists(marina.id())).isEmpty();
        assertThat(fakes.events.published(DomainEvent.PurchaseFinished.class)).hasSize(1);
    }
}
