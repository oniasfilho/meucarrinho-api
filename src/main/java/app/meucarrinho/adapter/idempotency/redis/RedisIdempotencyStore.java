package app.meucarrinho.adapter.idempotency.redis;

import app.meucarrinho.application.common.port.IdempotencyClaim;
import app.meucarrinho.application.common.port.IdempotencyError;
import app.meucarrinho.application.common.port.IdempotencyKey;
import app.meucarrinho.application.common.port.IdempotencyStore;
import app.meucarrinho.application.common.port.IdempotencyTtl;
import app.meucarrinho.application.common.port.RequestFingerprint;
import app.meucarrinho.application.common.port.StoredResponse;
import app.meucarrinho.domain.shared.Result;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * One Redis string per key, expiring by itself: a running claim lives for the lease ({@code SET NX PX}, which makes
 * the claim atomic), a completed response for the retention. Every Redis failure becomes
 * {@link IdempotencyError.Unavailable}.
 */
final class RedisIdempotencyStore implements IdempotencyStore {
    private static final String PREFIX = "carrinho:idempotency:";
    /** Deletes the key only while it still holds this request's running claim. */
    private static final RedisScript<Long> RELEASE = RedisScript.of(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end return 0", Long.class);
    /** A key can expire between a refused SET NX and the GET that follows; then the claim simply tries again. */
    private static final int CLAIM_ATTEMPTS = 3;

    private final RedisTemplate<String, byte[]> redis;
    private final IdempotencyTtl ttl;

    RedisIdempotencyStore(RedisTemplate<String, byte[]> redis, IdempotencyTtl ttl) {
        this.redis = redis;
        this.ttl = ttl;
    }

    @Override
    public Result<IdempotencyClaim, IdempotencyError> claim(IdempotencyKey key, RequestFingerprint request) {
        return guarded(() -> {
            byte[] running = RedisEntry.running(request);
            for (int attempt = 0; attempt < CLAIM_ATTEMPTS; attempt++) {
                if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(redisKey(key), running, ttl.lease()))) {
                    return new IdempotencyClaim.Acquired();
                }
                Optional<IdempotencyClaim> held = find(key, request);
                if (held.isPresent()) {
                    return held.get();
                }
            }
            throw new IllegalStateException("Key " + redisKey(key) + " kept expiring while being claimed");
        });
    }

    @Override
    public Result<Optional<IdempotencyClaim>, IdempotencyError> lookup(IdempotencyKey key, RequestFingerprint request) {
        return guarded(() -> find(key, request));
    }

    @Override
    public Result<@Nullable Void, IdempotencyError> complete(IdempotencyKey key, RequestFingerprint request,
            StoredResponse response) {
        return guarded(() -> {
            redis.opsForValue().set(redisKey(key), RedisEntry.completed(request, response), ttl.retention());
            return null;
        });
    }

    @Override
    public Result<@Nullable Void, IdempotencyError> release(IdempotencyKey key, RequestFingerprint request) {
        return guarded(() -> {
            redis.execute(RELEASE, List.of(redisKey(key)), (Object) RedisEntry.running(request));
            return null;
        });
    }

    private Optional<IdempotencyClaim> find(IdempotencyKey key, RequestFingerprint request) {
        byte[] stored = redis.opsForValue().get(redisKey(key));
        return stored == null ? Optional.empty() : Optional.of(RedisEntry.decode(stored).claimFor(request));
    }

    private static String redisKey(IdempotencyKey key) {
        return PREFIX + key.account() + ":" + key.value();
    }

    private static <T extends @Nullable Object> Result<T, IdempotencyError> guarded(Supplier<T> call) {
        try {
            return Result.ok(call.get());
        } catch (DataAccessException e) {
            return Result.err(new IdempotencyError.Unavailable("Redis: " + e.getClass().getSimpleName()));
        }
    }
}
