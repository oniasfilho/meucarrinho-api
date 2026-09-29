package app.meucarrinho.adapter.persistence.postgres;

import app.meucarrinho.application.accounts.port.AccountRepository;
import app.meucarrinho.testfixtures.accounts.AccountRepositoryContract;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PostgresAccountRepositoryContractTest extends AccountRepositoryContract {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");
    private static org.springframework.jdbc.core.JdbcTemplate jdbc;

    @BeforeAll
    static void setUpDatabase() {
        jdbc = PostgresTestDatabase.migrate(POSTGRES);
    }

    @BeforeEach
    void clearDatabase() {
        PostgresTestDatabase.clear(jdbc);
    }

    @Override
    protected AccountRepository createRepository() {
        return new PostgresAccountRepository(jdbc);
    }
}
