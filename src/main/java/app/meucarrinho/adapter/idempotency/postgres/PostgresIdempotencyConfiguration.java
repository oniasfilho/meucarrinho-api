package app.meucarrinho.adapter.idempotency.postgres;

import app.meucarrinho.application.common.port.Clock;
import app.meucarrinho.application.common.port.IdempotencyStore;
import app.meucarrinho.application.common.port.IdempotencyTtl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Selected alone by {@code carrinho.adapters.idempotency=postgres}. With {@code redis-postgres}, bootstrap's
 * composite builds this store through {@link #create} instead, so it never becomes a second port bean (ADR 0010).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "carrinho.adapters", name = "idempotency", havingValue = "postgres")
public class PostgresIdempotencyConfiguration {
    @Bean
    IdempotencyStore postgresIdempotencyStore(JdbcTemplate jdbc, Clock clock) {
        return create(jdbc, clock, IdempotencyTtl.STANDARD);
    }

    /** Builds the table store on Boot's {@link JdbcTemplate} without registering it as a bean. */
    public static IdempotencyStore create(JdbcTemplate jdbc, Clock clock, IdempotencyTtl ttl) {
        return new PostgresIdempotencyStore(jdbc, clock, ttl);
    }
}
