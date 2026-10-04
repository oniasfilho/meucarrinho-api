package app.meucarrinho.bootstrap;

import app.meucarrinho.adapter.idempotency.postgres.PostgresIdempotencyConfiguration;
import app.meucarrinho.adapter.idempotency.redis.RedisIdempotencyConfiguration;
import app.meucarrinho.api.rest.CurrentActor;
import app.meucarrinho.application.common.port.Clock;
import app.meucarrinho.application.common.port.IdempotencyStore;
import app.meucarrinho.application.common.port.IdempotencyTtl;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Set;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
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

    /**
     * Documents the {@code Idempotency-Key} header and its three codes (spec §13) on every mutating {@code /v1}
     * operation, mirroring {@link IdempotencyFilter}'s own rule for which calls it guards.
     */
    @Bean
    GlobalOpenApiCustomizer idempotencyOpenApiCustomizer() {
        Set<PathItem.HttpMethod> mutating = Set.of(PathItem.HttpMethod.POST, PathItem.HttpMethod.PUT,
                PathItem.HttpMethod.PATCH, PathItem.HttpMethod.DELETE);
        return openApi -> openApi.getPaths().forEach((path, item) -> {
            if (!path.startsWith("/v1/")) {
                return;
            }
            item.readOperationsMap().forEach((method, operation) -> {
                if (!mutating.contains(method)) {
                    return;
                }
                operation.addParametersItem(new Parameter().in("header").name(IdempotencyFilter.KEY_HEADER)
                        .required(false).schema(new StringSchema())
                        .description("Makes this call safe to retry (spec §7). 1 to 128 characters of "
                                + "A-Z a-z 0-9 . _ : -"));
                operation.getResponses()
                        .addApiResponse("409", new ApiResponse().description(
                                "IDEMPOTENCY_REQUEST_IN_PROGRESS: a call with this key is still running. "
                                        + "IDEMPOTENCY_KEY_REUSED: this key was used for a different request."))
                        .addApiResponse("503", new ApiResponse().description(
                                "DEPENDENCY_UNAVAILABLE: no idempotency store answered; retry with the same key."));
            });
        });
    }
}
