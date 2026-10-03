package app.meucarrinho.adapter.idempotency.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.application.common.port.IdempotencyError;
import app.meucarrinho.application.common.port.IdempotencyKey;
import app.meucarrinho.application.common.port.IdempotencyStore;
import app.meucarrinho.application.common.port.IdempotencyTtl;
import app.meucarrinho.application.common.port.RequestFingerprint;
import app.meucarrinho.testfixtures.TestIds;
import app.meucarrinho.testfixtures.common.IdempotencyStoreContract;
import app.meucarrinho.testfixtures.common.MutableClock;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** The contract on the V3 table, migrated by Flyway, plus the translation of a database that is not there. */
@Testcontainers(disabledWithoutDocker = true)
class PostgresIdempotencyStoreContractTest extends IdempotencyStoreContract {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");
    private static JdbcTemplate jdbc;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-03T12:00:00Z"));

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load()
                .migrate();
        jdbc = new JdbcTemplate(
                new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    @Override
    protected IdempotencyStore createStore(IdempotencyTtl ttl) {
        return PostgresIdempotencyConfiguration.create(jdbc, clock, ttl);
    }

    @Override
    protected void elapse(Duration duration) {
        clock.advance(duration);
    }

    @Test
    void a_database_that_is_not_there_is_unavailable() throws IOException {
        int closedPort;
        try (var socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        var nowhere = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:postgresql://localhost:" + closedPort + "/carrinho", "carrinho", "carrinho"));
        IdempotencyStore store = PostgresIdempotencyConfiguration.create(nowhere, clock, TTL);

        assertThat(store.claim(new IdempotencyKey(TestIds.accountId(), "retry-1"), new RequestFingerprint("c".repeat(64)))
                .errorOrThrow()).isInstanceOf(IdempotencyError.Unavailable.class);
    }
}
