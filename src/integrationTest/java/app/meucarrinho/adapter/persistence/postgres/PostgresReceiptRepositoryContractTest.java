package app.meucarrinho.adapter.persistence.postgres;

import app.meucarrinho.application.receipts.port.ReceiptRepository;
import app.meucarrinho.testfixtures.receipts.ReceiptRepositoryContract;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PostgresReceiptRepositoryContractTest extends ReceiptRepositoryContract {
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
    protected ReceiptRepository createRepository() {
        return new PostgresReceiptRepository(jdbc);
    }
}
