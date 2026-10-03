package app.meucarrinho.bootstrap;

import app.meucarrinho.application.common.port.Clock;
import java.time.Instant;

/** The wall clock. Not an adapter: there is nothing to swap, so no {@code carrinho.adapters} key (ADR 0007). */
final class SystemClock implements Clock {
    @Override
    public Instant now() {
        return Instant.now();
    }
}
