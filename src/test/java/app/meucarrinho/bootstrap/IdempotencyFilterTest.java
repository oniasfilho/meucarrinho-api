package app.meucarrinho.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.api.rest.RestTest;
import app.meucarrinho.api.rest.lists.CreateListRequest;
import app.meucarrinho.api.rest.lists.ListsApi;
import app.meucarrinho.application.common.port.IdempotencyKey;
import app.meucarrinho.application.common.port.RequestFingerprint;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.testfixtures.common.InMemoryIdempotencyStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** {@code Idempotency-Key} end to end: real filter, controllers and use cases on the fakes (spec §7, ADR 0010). */
class IdempotencyFilterTest extends RestTest {
    private static final String MILK = "{\"text\":\"2 leite 5,49\"}";

    @Autowired
    private InMemoryIdempotencyStore store;

    private AccountId marina;
    private UUID listId;

    @BeforeEach
    void aList() {
        marina = signIn("Marina");
        listId = client(ListsApi.class).create(new CreateListRequest(null, "Feira", null, null)).getBody().id();
    }

    private MvcTestResult addItem(String key, String ifMatch, String body) {
        return http.post().uri("/v1/lists/{id}/items", listId).header("Idempotency-Key", key).header("If-Match", ifMatch)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private int itemCount() {
        return client(ListsApi.class).get(listId).getBody().items().size();
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }

    @Test
    void a_retried_add_creates_one_item_and_replays_the_same_status_body_and_etag() throws Exception {
        String key = key();
        MvcTestResult first = addItem(key, "1", MILK);
        MvcTestResult retry = addItem(key, "1", MILK);

        assertThat(first.getResponse().getStatus()).isEqualTo(200);
        assertThat(retry.getResponse().getStatus()).isEqualTo(200);
        assertThat(retry.getResponse().getContentAsByteArray()).isEqualTo(first.getResponse().getContentAsByteArray());
        assertThat(retry.getResponse().getHeader("ETag")).isEqualTo("\"2\"").isEqualTo(first.getResponse().getHeader("ETag"));
        assertThat(retry.getResponse().getContentType()).isEqualTo(first.getResponse().getContentType());
        assertThat(first.getResponse().getHeader("Idempotent-Replayed")).isNull();
        assertThat(retry.getResponse().getHeader("Idempotent-Replayed")).isEqualTo("true");
        assertThat(itemCount()).isEqualTo(1);
    }

    @Test
    void without_a_key_the_same_retry_adds_a_second_item() {
        http.post().uri("/v1/lists/{id}/items", listId).header("If-Match", "1")
                .contentType(MediaType.APPLICATION_JSON).content(MILK).exchange();
        http.post().uri("/v1/lists/{id}/items", listId).header("If-Match", "2")
                .contentType(MediaType.APPLICATION_JSON).content(MILK).exchange();

        assertThat(itemCount()).isEqualTo(2);
    }

    @Test
    void a_retried_create_replays_201_and_the_same_location() {
        String key = key();
        String body = "{\"name\":\"Churrasco sábado\"}";
        MvcTestResult first = http.post().uri("/v1/lists").header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
        MvcTestResult retry = http.post().uri("/v1/lists").header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();

        assertThat(retry).hasStatus(HttpStatus.CREATED);
        assertThat(retry.getResponse().getHeader("Location")).isNotNull()
                .isEqualTo(first.getResponse().getHeader("Location"));
        assertThat(client(ListsApi.class).activeLists("active").lists())
                .filteredOn(card -> card.name().equals("Churrasco sábado")).hasSize(1);
    }

    @Test
    void the_same_key_for_a_different_request_is_refused_and_changes_nothing() {
        String key = key();
        addItem(key, "1", MILK);

        assertThat(addItem(key, "2", "{\"text\":\"cafe\"}")).hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("IDEMPOTENCY_KEY_REUSED");
        assertThat(itemCount()).isEqualTo(1);
    }

    @Test
    void a_retry_while_the_first_call_runs_is_told_to_wait() throws Exception {
        String key = key();
        store.claim(new IdempotencyKey(marina, key), fingerprint("POST", "/v1/lists/" + listId + "/items", "1", MILK))
                .orElseThrow();

        MvcTestResult retry = addItem(key, "1", MILK);

        assertThat(retry).hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("IDEMPOTENCY_REQUEST_IN_PROGRESS");
        assertThat(retry.getResponse().getHeader("Retry-After")).isEqualTo("1");
        assertThat(itemCount()).isZero();
    }

    @Test
    void failures_are_not_stored_so_the_client_can_fix_the_request_and_keep_its_key() {
        String key = key();
        assertThat(addItem(key, "7", MILK)).hasStatus(HttpStatus.PRECONDITION_FAILED);
        assertThat(addItem(key, "7", MILK)).as("runs again, still stale").hasStatus(HttpStatus.PRECONDITION_FAILED);

        assertThat(addItem(key, "1", MILK)).hasStatus(HttpStatus.OK);
        assertThat(addItem(key, "1", MILK)).hasStatus(HttpStatus.OK)
                .headers().hasValue("Idempotent-Replayed", "true");
        assertThat(itemCount()).isEqualTo(1);
    }

    @Test
    void keys_belong_to_one_account() {
        String key = key();
        addItem(key, "1", MILK);

        signIn("Jessica");
        UUID jessicasList = client(ListsApi.class).create(new CreateListRequest(null, "Mercado", null, null)).getBody().id();
        assertThat(http.post().uri("/v1/lists/{id}/items", jessicasList).header("Idempotency-Key", key)
                .header("If-Match", "1").contentType(MediaType.APPLICATION_JSON).content(MILK))
                .hasStatus(HttpStatus.OK).headers().doesNotContainHeader("Idempotent-Replayed");
    }

    @Test
    void a_malformed_key_is_a_malformed_request() {
        assertThat(addItem("not a key!", "1", MILK)).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("MALFORMED_REQUEST");
        assertThat(addItem("k".repeat(129), "1", MILK)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(itemCount()).isZero();
    }

    @Test
    void anonymous_calls_pass_through_to_the_401() {
        actor.signOut();

        assertThat(addItem(key(), "1", MILK)).hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.code").isEqualTo("UNAUTHENTICATED");
    }

    @Test
    void with_no_store_to_remember_the_call_it_does_not_run() {
        store.goDown();
        try {
            MvcTestResult result = addItem(key(), "1", MILK);

            assertThat(result).hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
                    .bodyJson().extractingPath("$.code").isEqualTo("DEPENDENCY_UNAVAILABLE");
            assertThat(result.getResponse().getHeader("Retry-After")).isEqualTo("1");
        } finally {
            store.comeBack();
        }
        assertThat(itemCount()).isZero();
    }

    /** What makes two requests the same: method, path and query, If-Match, and the body bytes. */
    private static RequestFingerprint fingerprint(String method, String path, String ifMatch, String body)
            throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        sha256.update((method + "\n" + path + "\n" + ifMatch + "\n").getBytes(StandardCharsets.UTF_8));
        sha256.update(body.getBytes(StandardCharsets.UTF_8));
        return new RequestFingerprint(HexFormat.of().formatHex(sha256.digest()));
    }
}
