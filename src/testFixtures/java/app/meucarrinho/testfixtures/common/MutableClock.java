package app.meucarrinho.testfixtures.common;

import app.meucarrinho.application.common.port.Clock;
import java.time.Duration;
import java.time.Instant;

public final class MutableClock implements Clock {
    private Instant now;

    public MutableClock(Instant start) {
        this.now = start;
    }

    @Override
    public Instant now() {
        return now;
    }

    public void set(Instant instant) {
        now = instant;
    }

    public void advance(Duration duration) {
        now = now.plus(duration);
    }
}
