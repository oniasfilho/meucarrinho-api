package app.meucarrinho.bootstrap;

import app.meucarrinho.adapter.idempotency.postgres.PostgresIdempotencyConfiguration;
import app.meucarrinho.adapter.idempotency.redis.RedisIdempotencyConfiguration;
import app.meucarrinho.api.rest.CurrentActor;
import app.meucarrinho.application.common.port.Clock;
import app.meucarrinho.application.common.port.IdempotencyStore;
import app.meucarrinho.application.common.port.IdempotencyTtl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** {@code Idempotency-Key} handling for every mutating call (spec §7, ADR 0010). */
@Configuration(proxyBeanMethods = false)
class IdempotencyConfiguration {
    /**
     * {@code carrinho.adapters.idempotency=redis-postgres}: Redis with the PostgreSQL table behind it. The two members
     * are built here, not registered, so the port still has exactly one bean (ADR 0007).
     */
    @Bean
    @ConditionalOnProperty(prefix = "carrinho.adapters", name = "idempotency", havingValue = "redis-postgres")
    IdempotencyStore redisWithPostgresFallbackIdempotencyStore(RedisConnectionFactory redis, JdbcTemplate jdbc,
            Clock clock) {
        return new FallbackIdempotencyStore(RedisIdempotencyConfiguration.create(redis, IdempotencyTtl.STANDARD),
                PostgresIdempotencyConfiguration.create(jdbc, clock, IdempotencyTtl.STANDARD));
    }

    /** Registered for every path with the default (lowest) order, so it runs after authentication (step 5). */
    @Bean
    IdempotencyFilter idempotencyFilter(IdempotencyStore store, CurrentActor actor,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver problems) {
        return new IdempotencyFilter(store, actor, problems);
    }
}
