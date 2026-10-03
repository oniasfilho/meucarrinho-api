package app.meucarrinho.adapter.idempotency.redis;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.application.common.port.IdempotencyError;
import app.meucarrinho.application.common.port.IdempotencyKey;
import app.meucarrinho.application.common.port.IdempotencyStore;
import app.meucarrinho.application.common.port.IdempotencyTtl;
import app.meucarrinho.application.common.port.RequestFingerprint;
import app.meucarrinho.testfixtures.TestIds;
import app.meucarrinho.testfixtures.common.IdempotencyStoreContract;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** The contract on Valkey, the Redis the local stack runs (spec §14), plus the translation of a dead server. */
@Testcontainers(disabledWithoutDocker = true)
class RedisIdempotencyStoreContractTest extends IdempotencyStoreContract {
    @Container
    private static final GenericContainer<?> VALKEY = new GenericContainer<>("valkey/valkey:8").withExposedPorts(6379);
    private static LettuceConnectionFactory connections;

    @BeforeAll
    static void connect() {
        connections = connect(VALKEY.getHost(), VALKEY.getMappedPort(6379));
    }

    @AfterAll
    static void disconnect() {
        connections.destroy();
    }

    private static LettuceConnectionFactory connect(String host, int port) {
        var factory = new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port));
        factory.afterPropertiesSet();
        factory.start();
        return factory;
    }

    @Override
    protected IdempotencyStore createStore(IdempotencyTtl ttl) {
        return RedisIdempotencyConfiguration.create(connections, ttl);
    }

    @Override
    protected void elapse(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @Test
    void a_server_that_is_not_there_is_unavailable() throws IOException {
        int closedPort;
        try (var socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        LettuceConnectionFactory nowhere = connect("localhost", closedPort);
        try {
            IdempotencyStore store = RedisIdempotencyConfiguration.create(nowhere, TTL);
            var key = new IdempotencyKey(TestIds.accountId(), "retry-1");

            assertThat(store.claim(key, new RequestFingerprint("c".repeat(64))).errorOrThrow())
                    .isInstanceOf(IdempotencyError.Unavailable.class);
        } finally {
            nowhere.destroy();
        }
    }
}
