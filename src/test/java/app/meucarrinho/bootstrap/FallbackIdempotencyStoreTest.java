package app.meucarrinho.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.application.common.port.IdempotencyClaim;
import app.meucarrinho.application.common.port.IdempotencyError;
import app.meucarrinho.application.common.port.IdempotencyKey;
import app.meucarrinho.application.common.port.IdempotencyStore;
import app.meucarrinho.application.common.port.IdempotencyTtl;
import app.meucarrinho.application.common.port.RequestFingerprint;
import app.meucarrinho.application.common.port.StoredResponse;
import app.meucarrinho.testfixtures.TestIds;
import app.meucarrinho.testfixtures.common.IdempotencyStoreContract;
import app.meucarrinho.testfixtures.common.InMemoryIdempotencyStore;
import app.meucarrinho.testfixtures.common.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Redis first, PostgreSQL when Redis is down (ADR 0010), on two fakes. */
class FallbackIdempotencyStoreTest {
    private static final RequestFingerprint REQUEST = new RequestFingerprint("a".repeat(64));
    private static final StoredResponse OK = new StoredResponse(200, Map.of("ETag", "\"2\""), "{}".getBytes());

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-03T12:00:00Z"));
    private final InMemoryIdempotencyStore redis = new InMemoryIdempotencyStore(clock, IdempotencyTtl.STANDARD);
    private final InMemoryIdempotencyStore postgres = new InMemoryIdempotencyStore(clock, IdempotencyTtl.STANDARD);
    private final IdempotencyStore store = new FallbackIdempotencyStore(redis, postgres);
    private final IdempotencyKey key = new IdempotencyKey(TestIds.accountId(), "retry-1");

    /** The composite is an IdempotencyStore like any other, with both members up. */
    @Nested
    class Contract extends IdempotencyStoreContract {
        private final MutableClock contractClock = new MutableClock(Instant.parse("2026-10-03T12:00:00Z"));

        @Override
        protected IdempotencyStore createStore(IdempotencyTtl ttl) {
            return new FallbackIdempotencyStore(new InMemoryIdempotencyStore(contractClock, ttl),
                    new InMemoryIdempotencyStore(contractClock, ttl));
        }

        @Override
        protected void elapse(Duration duration) {
            contractClock.advance(duration);
        }
    }

    @Test
    void uses_only_redis_while_it_is_up() {
        assertThat(store.claim(key, REQUEST).orElseThrow()).isEqualTo(new IdempotencyClaim.Acquired());
        store.complete(key, REQUEST, OK).orElseThrow();

        assertThat(redis.size()).isEqualTo(1);
        assertThat(postgres.size()).isZero();
        assertThat(store.claim(key, REQUEST).orElseThrow()).isEqualTo(new IdempotencyClaim.Completed(OK));
    }

    @Test
    void falls_back_to_postgres_while_redis_is_down() {
        redis.goDown();

        assertThat(store.claim(key, REQUEST).orElseThrow()).isEqualTo(new IdempotencyClaim.Acquired());
        store.complete(key, REQUEST, OK).orElseThrow();

        assertThat(postgres.size()).isEqualTo(1);
        assertThat(store.claim(key, REQUEST).orElseThrow()).isEqualTo(new IdempotencyClaim.Completed(OK));
    }

    @Test
    void a_response_stored_during_the_outage_still_replays_once_redis_is_back() {
        redis.goDown();
        store.claim(key, REQUEST).orElseThrow();
        store.complete(key, REQUEST, OK).orElseThrow();
        redis.comeBack();

        assertThat(store.claim(key, REQUEST).orElseThrow()).isEqualTo(new IdempotencyClaim.Completed(OK));
        assertThat(redis.size()).as("the Redis claim taken while looking was given back").isZero();
        assertThat(store.lookup(key, REQUEST).orElseThrow()).contains(new IdempotencyClaim.Completed(OK));
    }

    @Test
    void a_request_claimed_in_redis_and_completed_in_postgres_replays_after_its_lease() {
        store.claim(key, REQUEST).orElseThrow();
        redis.goDown();
        store.complete(key, REQUEST, OK).orElseThrow();
        redis.comeBack();

        assertThat(store.claim(key, REQUEST).orElseThrow()).as("Redis still holds the running claim")
                .isEqualTo(new IdempotencyClaim.InProgress());
        clock.advance(IdempotencyTtl.STANDARD.lease());
        assertThat(store.claim(key, REQUEST).orElseThrow()).isEqualTo(new IdempotencyClaim.Completed(OK));
    }

    @Test
    void release_frees_a_claim_taken_in_either_store() {
        redis.goDown();
        store.claim(key, REQUEST).orElseThrow();
        redis.comeBack();

        store.release(key, REQUEST).orElseThrow();

        assertThat(postgres.size()).isZero();
        assertThat(store.claim(key, REQUEST).orElseThrow()).isEqualTo(new IdempotencyClaim.Acquired());
    }

    @Test
    void is_unavailable_only_when_both_are_down() {
        redis.goDown();
        postgres.goDown();

        assertThat(store.claim(key, REQUEST).errorOrThrow()).isInstanceOf(IdempotencyError.Unavailable.class);
        assertThat(store.complete(key, REQUEST, OK).errorOrThrow()).isInstanceOf(IdempotencyError.Unavailable.class);
    }

    @Test
    void a_postgres_outage_does_not_stop_redis_from_serving() {
        postgres.goDown();

        assertThat(store.claim(key, REQUEST).orElseThrow()).isEqualTo(new IdempotencyClaim.Acquired());
        store.complete(key, REQUEST, OK).orElseThrow();
        assertThat(store.claim(key, REQUEST).orElseThrow()).isEqualTo(new IdempotencyClaim.Completed(OK));
    }
}
