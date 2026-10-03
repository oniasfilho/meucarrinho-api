package app.meucarrinho.bootstrap;

import app.meucarrinho.application.accounts.port.AccountRepository;
import app.meucarrinho.application.common.port.DomainEventPublisher;
import app.meucarrinho.application.common.port.UnitOfWork;
import app.meucarrinho.application.lists.port.ListQueries;
import app.meucarrinho.application.lists.port.ShoppingListRepository;
import app.meucarrinho.application.receipts.port.ReceiptRepository;
import app.meucarrinho.testfixtures.InMemoryCore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Stands in for {@code carrinho.adapters.persistence=postgres}: the testFixtures fakes, enlisted in one in-memory
 * unit of work by {@link InMemoryCore}. Clock and IdGenerator stay the real ones from {@link CoreConfiguration}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class InMemoryPortsConfiguration {
    @Bean
    InMemoryCore inMemoryCore() {
        return new InMemoryCore();
    }

    @Bean
    ShoppingListRepository inMemoryShoppingListRepository(InMemoryCore core) {
        return core.listRepository;
    }

    @Bean
    ListQueries inMemoryListQueries(InMemoryCore core) {
        return core.listQueries;
    }

    @Bean
    ReceiptRepository inMemoryReceiptRepository(InMemoryCore core) {
        return core.receiptRepository;
    }

    @Bean
    AccountRepository inMemoryAccountRepository(InMemoryCore core) {
        return core.accountRepository;
    }

    @Bean
    UnitOfWork inMemoryUnitOfWork(InMemoryCore core) {
        return core.unitOfWork;
    }

    @Bean
    DomainEventPublisher recordingDomainEventPublisher(InMemoryCore core) {
        return core.events;
    }
}
