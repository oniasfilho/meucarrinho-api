package app.meucarrinho.application.common.port;

import app.meucarrinho.domain.shared.Result;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Remembers the outcome of a mutating call made with an {@code Idempotency-Key} (spec §7), so a retry of the same
 * request gets the first response back instead of running again. A key is first claimed for a short lease while the
 * request runs, then holds the completed response for the retention period. Implementations make {@link #claim}
 * atomic: of several simultaneous claims of one free key, exactly one is {@link IdempotencyClaim.Acquired}.
 */
public interface IdempotencyStore {
    /**
     * Takes a free key for this request, or reports what holds it: {@link IdempotencyClaim.InProgress} or
     * {@link IdempotencyClaim.Completed} for the same request, {@link IdempotencyClaim.KeyReused} for another one.
     */
    Result<IdempotencyClaim, IdempotencyError> claim(IdempotencyKey key, RequestFingerprint request);

    /** Like {@link #claim} but never takes the key; empty when the key is free. Never returns {@code Acquired}. */
    Result<Optional<IdempotencyClaim>, IdempotencyError> lookup(IdempotencyKey key, RequestFingerprint request);

    /**
     * Stores the response for the retention period, whether or not this store holds the claim (a fallback store may
     * complete what another one claimed).
     */
    Result<@Nullable Void, IdempotencyError> complete(IdempotencyKey key, RequestFingerprint request, StoredResponse response);

    /** Frees a claim this request still holds, so a retry runs; leaves completed responses and other claims alone. */
    Result<@Nullable Void, IdempotencyError> release(IdempotencyKey key, RequestFingerprint request);
}
