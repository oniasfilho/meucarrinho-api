package app.meucarrinho.adapter.persistence.postgres;

import app.meucarrinho.application.accounts.port.AccountRepository;
import app.meucarrinho.application.lists.port.ListQueries;
import app.meucarrinho.application.lists.port.ShoppingListRepository;
import app.meucarrinho.application.receipts.port.ReceiptRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnProperty(prefix = "carrinho.adapters", name = "persistence", havingValue = "postgres")
class PostgresPersistenceConfiguration {
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
