package app.meucarrinho.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.application.common.port.IdempotencyClaim;
import app.meucarrinho.application.common.port.IdempotencyKey;
import app.meucarrinho.application.common.port.IdempotencyStore;
import app.meucarrinho.application.common.port.RequestFingerprint;
import app.meucarrinho.application.common.port.StoredResponse;
import app.meucarrinho.testfixtures.TestIds;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** The default {@code carrinho.adapters.idempotency=redis-postgres} boots and keeps keys in Valkey (ADR 0010). */
@SpringBootTest(classes = MeuCarrinhoApplication.class)
@Testcontainers(disabledWithoutDocker = true)
class IdempotencyWiringIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    @Container
    private static final GenericContainer<?> VALKEY = new GenericContainer<>("valkey/valkey:8").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.modulith.events.jdbc.schema-initialization.enabled", () -> false);
        properties.add("spring.aop.proxy-target-class", () -> false);
        properties.add("spring.data.redis.url",
                () -> "redis://" + VALKEY.getHost() + ":" + VALKEY.getMappedPort(6379));
    }

    @Autowired
    private IdempotencyStore store;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void the_default_store_is_redis_with_postgres_behind_it() {
        assertThat(store).isInstanceOf(FallbackIdempotencyStore.class);

        var key = new IdempotencyKey(TestIds.accountId(), "retry-1");
        var request = new RequestFingerprint("d".repeat(64));
        var response = new StoredResponse(200, Map.of("ETag", "\"2\""), "{}".getBytes());
        assertThat(store.claim(key, request).orElseThrow()).isEqualTo(new IdempotencyClaim.Acquired());
        store.complete(key, request, response).orElseThrow();

        assertThat(store.claim(key, request).orElseThrow()).isEqualTo(new IdempotencyClaim.Completed(response));
        assertThat(redis.hasKey("carrinho:idempotency:" + key.account() + ":retry-1")).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM idempotency_keys", Integer.class)).isZero();
    }
}
