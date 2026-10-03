package app.meucarrinho.testfixtures.common;

import app.meucarrinho.application.common.port.IdempotencyStore;
import app.meucarrinho.application.common.port.IdempotencyTtl;
import java.time.Duration;
import java.time.Instant;

class InMemoryIdempotencyStoreContractTest extends IdempotencyStoreContract {
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-03T12:00:00Z"));

    @Override
    protected IdempotencyStore createStore(IdempotencyTtl ttl) {
        return new InMemoryIdempotencyStore(clock, ttl);
    }

    @Override
    protected void elapse(Duration duration) {
        clock.advance(duration);
    }
}
