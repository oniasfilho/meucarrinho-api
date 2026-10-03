package app.meucarrinho.adapter.idempotency.postgres;

import app.meucarrinho.application.common.port.Clock;
import app.meucarrinho.application.common.port.IdempotencyClaim;
import app.meucarrinho.application.common.port.IdempotencyError;
import app.meucarrinho.application.common.port.IdempotencyKey;
import app.meucarrinho.application.common.port.IdempotencyStore;
import app.meucarrinho.application.common.port.IdempotencyTtl;
import app.meucarrinho.application.common.port.RequestFingerprint;
import app.meucarrinho.application.common.port.StoredResponse;
import app.meucarrinho.domain.shared.Result;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * One row per key in {@code idempotency_keys}. A claim is a single upsert that only takes over a row whose time has
 * passed, so it is atomic. Expiry follows the core's {@link Clock}. Every database failure becomes
 * {@link IdempotencyError.Unavailable}.
 */
final class PostgresIdempotencyStore implements IdempotencyStore {
    private static final String CLAIM = """
            INSERT INTO idempotency_keys (account_id, idempotency_key, fingerprint, expires_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (account_id, idempotency_key) DO UPDATE
            SET fingerprint = EXCLUDED.fingerprint, response_status = NULL, response_headers = NULL,
                response_body = NULL, expires_at = EXCLUDED.expires_at
            WHERE idempotency_keys.expires_at <= ?""";
    private static final String COMPLETE = """
            INSERT INTO idempotency_keys (account_id, idempotency_key, fingerprint, response_status, response_headers,
                response_body, expires_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (account_id, idempotency_key) DO UPDATE
            SET fingerprint = EXCLUDED.fingerprint, response_status = EXCLUDED.response_status,
                response_headers = EXCLUDED.response_headers, response_body = EXCLUDED.response_body,
                expires_at = EXCLUDED.expires_at""";
    private static final String RELEASE = """
            DELETE FROM idempotency_keys
            WHERE account_id = ? AND idempotency_key = ? AND fingerprint = ? AND response_status IS NULL""";
    private static final String FIND = """
            SELECT fingerprint, response_status, response_headers, response_body FROM idempotency_keys
            WHERE account_id = ? AND idempotency_key = ? AND expires_at > ?""";
    /** A row can expire between a refused claim and the read that follows; then the claim simply tries again. */
    private static final int CLAIM_ATTEMPTS = 3;

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final IdempotencyTtl ttl;

    PostgresIdempotencyStore(JdbcTemplate jdbc, Clock clock, IdempotencyTtl ttl) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.ttl = ttl;
    }

    @Override
    public Result<IdempotencyClaim, IdempotencyError> claim(IdempotencyKey key, RequestFingerprint request) {
        return guarded(() -> {
            for (int attempt = 0; attempt < CLAIM_ATTEMPTS; attempt++) {
                Instant now = clock.now();
                int taken = jdbc.update(CLAIM, key.account().value(), key.value(), request.value(),
                        Timestamp.from(now.plus(ttl.lease())), Timestamp.from(now));
                if (taken == 1) {
                    return new IdempotencyClaim.Acquired();
                }
                Optional<IdempotencyClaim> held = find(key, request);
                if (held.isPresent()) {
                    return held.get();
                }
            }
            throw new IllegalStateException("Idempotency key kept expiring while being claimed");
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
            jdbc.update(COMPLETE, key.account().value(), key.value(), request.value(), response.status(),
                    encode(response.headers()), response.body(), Timestamp.from(clock.now().plus(ttl.retention())));
            return null;
        });
    }

    @Override
    public Result<@Nullable Void, IdempotencyError> release(IdempotencyKey key, RequestFingerprint request) {
        return guarded(() -> {
            jdbc.update(RELEASE, key.account().value(), key.value(), request.value());
            return null;
        });
    }

    private Optional<IdempotencyClaim> find(IdempotencyKey key, RequestFingerprint request) {
        List<IdempotencyClaim> rows = jdbc.query(FIND, (row, n) -> claimFor(row, request),
                key.account().value(), key.value(), Timestamp.from(clock.now()));
        return rows.stream().findFirst();
    }

    private static IdempotencyClaim claimFor(ResultSet row, RequestFingerprint request) throws SQLException {
        if (!row.getString("fingerprint").equals(request.value())) {
            return new IdempotencyClaim.KeyReused();
        }
        int status = row.getInt("response_status");
        if (row.wasNull()) {
            return new IdempotencyClaim.InProgress();
        }
        return new IdempotencyClaim.Completed(
                new StoredResponse(status, decode(row.getString("response_headers")), row.getBytes("response_body")));
    }

    /** Header names and values never contain line breaks (RFC 9110), so one header per line is unambiguous. */
    private static String encode(Map<String, String> headers) {
        return headers.entrySet().stream()
                .map(header -> header.getKey() + ": " + header.getValue())
                .collect(Collectors.joining("\n"));
    }

    private static Map<String, String> decode(String headers) {
        Map<String, String> decoded = new LinkedHashMap<>();
        for (String line : headers.split("\n")) {
            int colon = line.indexOf(": ");
            if (colon > 0) {
                decoded.put(line.substring(0, colon), line.substring(colon + 2));
            }
        }
        return decoded;
    }

    private static <T extends @Nullable Object> Result<T, IdempotencyError> guarded(Supplier<T> call) {
        try {
            return Result.ok(call.get());
        } catch (DataAccessException e) {
            return Result.err(new IdempotencyError.Unavailable("PostgreSQL: " + e.getClass().getSimpleName()));
        }
    }
}
