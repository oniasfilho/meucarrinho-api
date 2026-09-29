package app.meucarrinho.adapter.persistence.postgres;

import app.meucarrinho.application.accounts.port.AccountRepository;
import app.meucarrinho.application.common.port.DomainEventPublisher;
import app.meucarrinho.application.common.port.UnitOfWork;
import app.meucarrinho.application.lists.port.ListQueries;
import app.meucarrinho.application.lists.port.ShoppingListRepository;
import app.meucarrinho.application.receipts.port.ReceiptRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@ConditionalOnProperty(prefix = "carrinho.adapters", name = "persistence", havingValue = "postgres")
class PostgresPersistenceConfiguration {
    @Bean
    UnitOfWork postgresUnitOfWork(TransactionTemplate transactions) {
        return new SpringTransactionUnitOfWork(transactions);
    }

    @Bean
    DomainEventPublisher postgresDomainEventPublisher(ApplicationEventPublisher events) {
        return new SpringModulithDomainEventPublisher(events);
    }

    @Bean
    AccountRepository postgresAccountRepository(JdbcTemplate jdbc) {
        return new PostgresAccountRepository(jdbc);
    }

    @Bean
    ShoppingListRepository postgresShoppingListRepository(JdbcTemplate jdbc) {
        return new PostgresShoppingListRepository(jdbc);
    }

    @Bean
    ReceiptRepository postgresReceiptRepository(JdbcTemplate jdbc) {
        return new PostgresReceiptRepository(jdbc);
    }

    @Bean
    ListQueries postgresListQueries(ShoppingListRepository lists, JdbcTemplate jdbc) {
        return new PostgresListQueries(lists, jdbc);
    }
}
