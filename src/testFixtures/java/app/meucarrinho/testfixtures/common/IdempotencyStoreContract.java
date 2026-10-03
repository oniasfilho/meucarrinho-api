package app.meucarrinho.testfixtures.common;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.application.common.port.IdempotencyClaim;
import app.meucarrinho.application.common.port.IdempotencyKey;
import app.meucarrinho.application.common.port.IdempotencyStore;
import app.meucarrinho.application.common.port.IdempotencyTtl;
import app.meucarrinho.application.common.port.RequestFingerprint;
import app.meucarrinho.application.common.port.StoredResponse;
import app.meucarrinho.testfixtures.TestIds;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What every {@link IdempotencyStore} must do (spec §7): claim a key once, hand the stored response back to a retry
 * of the same request, refuse the key for a different request, and forget it after its lease or retention ends.
 * Durations are short so that an adapter on real time ({@link #elapse} sleeps) stays fast.
 */
public abstract class IdempotencyStoreContract {
    protected static final IdempotencyTtl TTL = new IdempotencyTtl(Duration.ofMillis(400), Duration.ofMillis(1200));

    private static final RequestFingerprint REQUEST = new RequestFingerprint("a".repeat(64));
    private static final RequestFingerprint OTHER_REQUEST = new RequestFingerprint("b".repeat(64));

    private IdempotencyStore store;
    private IdempotencyKey key;

    /** A store using {@code ttl}, empty or at least holding none of the keys this contract makes up. */
    protected abstract IdempotencyStore createStore(IdempotencyTtl ttl);

    /** Lets {@code duration} pass for the store: advance its clock, or sleep if it uses real time. */
    protected abstract void elapse(Duration duration);

    @BeforeEach
    void setUp() {
        store = createStore(TTL);
        key = newKey();
    }

    private static IdempotencyKey newKey() {
        return new IdempotencyKey(TestIds.accountId(), UUID.randomUUID().toString());
    }

    private static StoredResponse created() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("ETag", "\"7\"");
        headers.put("Location", "/v1/lists/0199a1b2-0000-7000-8000-000000000001");
        return new StoredResponse(201, headers, "{\"name\":\"Feira de domingo – açaí\"}".getBytes(StandardCharsets.UTF_8));
    }

    private IdempotencyClaim claim(IdempotencyKey key, RequestFingerprint request) {
        return store.claim(key, request).orElseThrow();
    }

    @Test
    void the_first_claim_acquires_the_key() {
        assertThat(claim(key, REQUEST)).isEqualTo(new IdempotencyClaim.Acquired());
    }

    @Test
    void a_claim_while_the_first_request_runs_sees_it_in_progress() {
        claim(key, REQUEST);

        assertThat(claim(key, REQUEST)).isEqualTo(new IdempotencyClaim.InProgress());
    }

    @Test
    void a_retry_after_completion_gets_the_stored_response_byte_for_byte() {
        claim(key, REQUEST);
        store.complete(key, REQUEST, created()).orElseThrow();

        assertThat(claim(key, REQUEST)).isEqualTo(new IdempotencyClaim.Completed(created()));
        assertThat(claim(key, REQUEST)).as("replays as often as asked").isEqualTo(new IdempotencyClaim.Completed(created()));
    }

    @Test
    void stores_a_response_without_headers_or_body() {
        claim(key, REQUEST);
        StoredResponse noContent = new StoredResponse(204, Map.of(), new byte[0]);
        store.complete(key, REQUEST, noContent).orElseThrow();

        assertThat(claim(key, REQUEST)).isEqualTo(new IdempotencyClaim.Completed(noContent));
    }

    @Test
    void the_same_key_for_a_different_request_is_reused_while_running_and_after_completion() {
        claim(key, REQUEST);
        assertThat(claim(key, OTHER_REQUEST)).isEqualTo(new IdempotencyClaim.KeyReused());

        store.complete(key, REQUEST, created()).orElseThrow();
        assertThat(claim(key, OTHER_REQUEST)).isEqualTo(new IdempotencyClaim.KeyReused());
        assertThat(claim(key, REQUEST)).as("the original still replays").isEqualTo(new IdempotencyClaim.Completed(created()));
    }

    @Test
    void keys_belong_to_one_account() {
        claim(key, REQUEST);
        store.complete(key, REQUEST, created()).orElseThrow();

        IdempotencyKey sameValueOtherAccount = new IdempotencyKey(TestIds.accountId(), key.value());
        assertThat(claim(sameValueOtherAccount, OTHER_REQUEST)).isEqualTo(new IdempotencyClaim.Acquired());
    }

    @Test
    void release_frees_a_running_claim_so_the_retry_runs() {
        claim(key, REQUEST);
        store.release(key, REQUEST).orElseThrow();

        assertThat(claim(key, OTHER_REQUEST)).isEqualTo(new IdempotencyClaim.Acquired());
    }

    @Test
    void release_never_drops_a_completed_response_or_someone_elses_claim() {
        claim(key, REQUEST);
        store.release(key, OTHER_REQUEST).orElseThrow();
        assertThat(claim(key, REQUEST)).isEqualTo(new IdempotencyClaim.InProgress());

        store.complete(key, REQUEST, created()).orElseThrow();
        store.release(key, REQUEST).orElseThrow();
        assertThat(claim(key, REQUEST)).isEqualTo(new IdempotencyClaim.Completed(created()));

        store.release(newKey(), REQUEST).orElseThrow();
    }

    @Test
    void a_claim_that_is_never_completed_lapses_after_its_lease() {
        claim(key, REQUEST);
        elapse(TTL.lease().plusMillis(200));

        assertThat(claim(key, OTHER_REQUEST)).isEqualTo(new IdempotencyClaim.Acquired());
    }

    @Test
    void a_completed_response_is_kept_past_the_lease_and_forgotten_after_retention() {
        claim(key, REQUEST);
        store.complete(key, REQUEST, created()).orElseThrow();

        elapse(TTL.lease().plusMillis(200));
        assertThat(claim(key, REQUEST)).isEqualTo(new IdempotencyClaim.Completed(created()));

        elapse(TTL.retention());
        assertThat(claim(key, OTHER_REQUEST)).isEqualTo(new IdempotencyClaim.Acquired());
    }

    @Test
    void complete_stores_the_response_even_without_a_claim_in_this_store() {
        store.complete(key, REQUEST, created()).orElseThrow();

        assertThat(claim(key, REQUEST)).isEqualTo(new IdempotencyClaim.Completed(created()));
    }

    @Test
    void lookup_reports_what_a_claim_would_see_without_taking_the_key() {
        assertThat(store.lookup(key, REQUEST).orElseThrow()).isEmpty();
        assertThat(claim(key, REQUEST)).as("lookup took nothing").isEqualTo(new IdempotencyClaim.Acquired());

        assertThat(store.lookup(key, REQUEST).orElseThrow()).contains(new IdempotencyClaim.InProgress());
        assertThat(store.lookup(key, OTHER_REQUEST).orElseThrow()).contains(new IdempotencyClaim.KeyReused());

        store.complete(key, REQUEST, created()).orElseThrow();
        assertThat(store.lookup(key, REQUEST).orElseThrow()).contains(new IdempotencyClaim.Completed(created()));
    }

    @Test
    void only_one_of_many_simultaneous_claims_acquires_the_key() throws Exception {
        int callers = 8;
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<IdempotencyClaim>> claims = new ArrayList<>();
        for (int i = 0; i < callers; i++) {
            claims.add(() -> {
                start.await();
                return store.claim(key, REQUEST).orElseThrow();
            });
        }
        List<IdempotencyClaim> outcomes = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(callers)) {
            List<Future<IdempotencyClaim>> futures = new ArrayList<>();
            for (Callable<IdempotencyClaim> claim : claims) {
                futures.add(pool.submit(claim));
            }
            start.countDown();
            for (Future<IdempotencyClaim> future : futures) {
                outcomes.add(future.get());
            }
        }

        assertThat(outcomes).filteredOn(new IdempotencyClaim.Acquired()::equals).hasSize(1);
        assertThat(outcomes).filteredOn(new IdempotencyClaim.InProgress()::equals).hasSize(callers - 1);
    }

    @Test
    void lookup_of_an_unknown_key_is_empty() {
        assertThat(store.lookup(newKey(), REQUEST).orElseThrow()).isEqualTo(Optional.empty());
    }
}
