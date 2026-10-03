package app.meucarrinho.bootstrap;

import app.meucarrinho.application.common.port.Clock;
import app.meucarrinho.application.common.port.IdGenerator;
import app.meucarrinho.domain.shared.Uuid7;
import java.util.UUID;
import java.util.random.RandomGenerator;

/** Server-minted UUIDv7, stamped with the core's clock; production passes a {@code SecureRandom} (spec §4, §12). */
final class Uuid7IdGenerator implements IdGenerator {
    private final Clock clock;
    private final RandomGenerator random;

    Uuid7IdGenerator(Clock clock, RandomGenerator random) {
        this.clock = clock;
        this.random = random;
    }

    @Override
    public UUID next() {
        return Uuid7.generate(clock.now().toEpochMilli(), random);
    }
}
