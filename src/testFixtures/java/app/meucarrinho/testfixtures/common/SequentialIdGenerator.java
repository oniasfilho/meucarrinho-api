package app.meucarrinho.testfixtures.common;

import app.meucarrinho.application.common.port.IdGenerator;
import app.meucarrinho.domain.shared.Uuid7;
import java.util.SplittableRandom;
import java.util.UUID;

public final class SequentialIdGenerator implements IdGenerator {
    private final SplittableRandom random;
    private long nextMillis;

    public SequentialIdGenerator() {
        this(1_790_000_000_000L, 7);
    }

    public SequentialIdGenerator(long startMillis, long seed) {
        this.nextMillis = startMillis;
        this.random = new SplittableRandom(seed);
    }

    @Override
    public synchronized UUID next() {
        return Uuid7.generate(nextMillis++, random);
    }
}
