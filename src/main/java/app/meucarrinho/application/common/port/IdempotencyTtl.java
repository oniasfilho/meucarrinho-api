package app.meucarrinho.application.common.port;

import java.time.Duration;

/**
 * How long an {@link IdempotencyStore} keeps a key: {@code lease} while the request runs, so a crashed request
 * frees its key soon, and {@code retention} once it has completed.
 */
public record IdempotencyTtl(Duration lease, Duration retention) {
    /** Spec §7 keeps keys 24 h; a lease outlives the slowest request (sync, 20 s, spec §13). */
    public static final IdempotencyTtl STANDARD = new IdempotencyTtl(Duration.ofSeconds(60), Duration.ofHours(24));

    public IdempotencyTtl {
        if (lease.isNegative() || lease.isZero() || retention.compareTo(lease) < 0) {
            throw new IllegalArgumentException("lease must be positive and no longer than retention");
        }
    }
}
