package app.meucarrinho.testfixtures.common;

import app.meucarrinho.application.common.port.Clock;
import app.meucarrinho.application.common.port.IdempotencyClaim;
import app.meucarrinho.application.common.port.IdempotencyError;
import app.meucarrinho.application.common.port.IdempotencyKey;
import app.meucarrinho.application.common.port.IdempotencyStore;
import app.meucarrinho.application.common.port.IdempotencyTtl;
import app.meucarrinho.application.common.port.RequestFingerprint;
import app.meucarrinho.application.common.port.StoredResponse;
import app.meucarrinho.domain.shared.Result;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** The idempotency store in a map, on the given clock. {@link #goDown} makes every call fail as an outage would. */
public final class InMemoryIdempotencyStore implements IdempotencyStore {
    private record Entry(RequestFingerprint request, @Nullable StoredResponse response, Instant expiresAt) {}

    private final Clock clock;
    private final IdempotencyTtl ttl;
    private final Map<IdempotencyKey, Entry> entries = new HashMap<>();
    private boolean down;

    public InMemoryIdempotencyStore(Clock clock, IdempotencyTtl ttl) {
        this.clock = clock;
        this.ttl = ttl;
    }

    public synchronized void goDown() {
        down = true;
    }

    public synchronized void comeBack() {
        down = false;
    }

    public synchronized int size() {
        return entries.size();
    }

    @Override
    public synchronized Result<IdempotencyClaim, IdempotencyError> claim(IdempotencyKey key, RequestFingerprint request) {
        if (down) {
            return unavailable();
        }
        Optional<IdempotencyClaim> held = find(key, request);
        if (held.isPresent()) {
            return Result.ok(held.get());
        }
        entries.put(key, new Entry(request, null, clock.now().plus(ttl.lease())));
        return Result.ok(new IdempotencyClaim.Acquired());
    }

    @Override
    public synchronized Result<Optional<IdempotencyClaim>, IdempotencyError> lookup(IdempotencyKey key,
            RequestFingerprint request) {
        return down ? unavailable() : Result.ok(find(key, request));
    }

    @Override
    public synchronized Result<@Nullable Void, IdempotencyError> complete(IdempotencyKey key, RequestFingerprint request,
            StoredResponse response) {
        if (down) {
            return unavailable();
        }
        entries.put(key, new Entry(request, response, clock.now().plus(ttl.retention())));
        return Result.ok();
    }

    @Override
    public synchronized Result<@Nullable Void, IdempotencyError> release(IdempotencyKey key, RequestFingerprint request) {
        if (down) {
            return unavailable();
        }
        entries.computeIfPresent(key, (k, entry) ->
                entry.response() == null && entry.request().equals(request) ? null : entry);
        return Result.ok();
    }

    private Optional<IdempotencyClaim> find(IdempotencyKey key, RequestFingerprint request) {
        Entry entry = entries.get(key);
        if (entry == null || !entry.expiresAt().isAfter(clock.now())) {
            return Optional.empty();
        }
        if (!entry.request().equals(request)) {
            return Optional.of(new IdempotencyClaim.KeyReused());
        }
        StoredResponse response = entry.response();
        return Optional.of(response == null ? new IdempotencyClaim.InProgress() : new IdempotencyClaim.Completed(response));
    }

    private static <T> Result<T, IdempotencyError> unavailable() {
        return Result.err(new IdempotencyError.Unavailable("in-memory store is down"));
    }
}
