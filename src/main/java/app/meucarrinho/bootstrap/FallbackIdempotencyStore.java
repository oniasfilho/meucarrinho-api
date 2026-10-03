package app.meucarrinho.bootstrap;

import app.meucarrinho.application.common.port.IdempotencyClaim;
import app.meucarrinho.application.common.port.IdempotencyError;
import app.meucarrinho.application.common.port.IdempotencyKey;
import app.meucarrinho.application.common.port.IdempotencyStore;
import app.meucarrinho.application.common.port.RequestFingerprint;
import app.meucarrinho.application.common.port.StoredResponse;
import app.meucarrinho.domain.shared.Result;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis first, PostgreSQL when Redis is unavailable (ADR 0010). It lives in bootstrap because adapters may not
 * reference each other (spec §11). A key claimed in Redis is also looked up in the fallback, so an entry written there
 * during an outage still replays once Redis is back.
 */
final class FallbackIdempotencyStore implements IdempotencyStore {
    private static final Logger LOG = LoggerFactory.getLogger(FallbackIdempotencyStore.class);

    private final IdempotencyStore primary;
    private final IdempotencyStore fallback;
    private final AtomicBoolean primaryDown = new AtomicBoolean();

    FallbackIdempotencyStore(IdempotencyStore primary, IdempotencyStore fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public Result<IdempotencyClaim, IdempotencyError> claim(IdempotencyKey key, RequestFingerprint request) {
        Result<IdempotencyClaim, IdempotencyError> claimed = primary.claim(key, request);
        if (failed(claimed)) {
            return fallback.claim(key, request);
        }
        if (!(claimed.orElseThrow() instanceof IdempotencyClaim.Acquired)) {
            return claimed;
        }
        Optional<IdempotencyClaim> heldInFallback = switch (fallback.lookup(key, request)) {
            case Result.Ok<Optional<IdempotencyClaim>, IdempotencyError> ok -> ok.value();
            case Result.Err<Optional<IdempotencyClaim>, IdempotencyError> err -> Optional.empty();
        };
        if (heldInFallback.isEmpty()) {
            return claimed;
        }
        primary.release(key, request);
        return Result.ok(heldInFallback.get());
    }

    @Override
    public Result<Optional<IdempotencyClaim>, IdempotencyError> lookup(IdempotencyKey key, RequestFingerprint request) {
        Result<Optional<IdempotencyClaim>, IdempotencyError> found = primary.lookup(key, request);
        if (failed(found) || found.orElseThrow().isEmpty()) {
            return fallback.lookup(key, request);
        }
        return found;
    }

    @Override
    public Result<@Nullable Void, IdempotencyError> complete(IdempotencyKey key, RequestFingerprint request,
            StoredResponse response) {
        Result<@Nullable Void, IdempotencyError> completed = primary.complete(key, request, response);
        return failed(completed) ? fallback.complete(key, request, response) : completed;
    }

    /** Releases in both stores: the claim may have been taken in the fallback while the primary was down. */
    @Override
    public Result<@Nullable Void, IdempotencyError> release(IdempotencyKey key, RequestFingerprint request) {
        Result<@Nullable Void, IdempotencyError> released = primary.release(key, request);
        Result<@Nullable Void, IdempotencyError> releasedInFallback = fallback.release(key, request);
        return failed(released) ? releasedInFallback : released;
    }

    /** Reports whether the primary failed, logging only when it goes down or comes back. */
    private boolean failed(Result<?, IdempotencyError> result) {
        if (result instanceof Result.Err<?, IdempotencyError>(IdempotencyError.Unavailable(String reason))) {
            if (primaryDown.compareAndSet(false, true)) {
                LOG.warn("Idempotency store unavailable ({}); using the fallback until it recovers", reason);
            }
            return true;
        }
        if (primaryDown.compareAndSet(true, false)) {
            LOG.info("Idempotency store recovered");
        }
        return false;
    }
}
