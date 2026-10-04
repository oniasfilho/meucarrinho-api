package app.meucarrinho.api.rest;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.api.rest.lists.CreateListRequest;
import app.meucarrinho.api.rest.lists.ListsApi;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * A malformed or hostile request at every endpoint never leaks a stack trace, an SQL keyword or a package name
 * (spec §13). Every mutating operation and every templated path is listed once in {@link #OPERATIONS}, so a new
 * endpoint is covered the day it is added here.
 */
class HostileInputTest extends RestTest {
    private static final String SQLI = "'; DROP TABLE lists; --";
    private static final String HOSTILE_PATH_SEGMENT = "' OR 1=1 OR '1'='1";
    private static final List<String> LEAKS = List.of("Exception", "Caused by", "\tat ", "app.meucarrinho",
            "org.postgresql", "SQLSTATE", "DROP TABLE", "SELECT ", "java.lang.", "java.sql.");

    private record Op(HttpMethod method, String uri, int pathVars, boolean hasBody) {}

    private static final List<Op> OPERATIONS = List.of(
            new Op(HttpMethod.GET, "/v1/lists", 0, false),
            new Op(HttpMethod.POST, "/v1/lists", 0, true),
            new Op(HttpMethod.GET, "/v1/lists/{id}", 1, false),
            new Op(HttpMethod.PATCH, "/v1/lists/{id}", 1, true),
            new Op(HttpMethod.DELETE, "/v1/lists/{id}", 1, false),
            new Op(HttpMethod.POST, "/v1/lists/{id}/restore", 1, false),
            new Op(HttpMethod.POST, "/v1/lists/{id}/duplicate", 1, true),
            new Op(HttpMethod.POST, "/v1/lists/{id}/finish", 1, true),
            new Op(HttpMethod.POST, "/v1/lists/{id}/items", 1, true),
            new Op(HttpMethod.PATCH, "/v1/lists/{id}/items/{itemId}", 2, true),
            new Op(HttpMethod.PUT, "/v1/lists/{id}/items/{itemId}/picked", 2, true),
            new Op(HttpMethod.DELETE, "/v1/lists/{id}/items/{itemId}", 2, false),
            new Op(HttpMethod.POST, "/v1/lists/{id}/items/{itemId}/restore", 2, false),
            new Op(HttpMethod.POST, "/v1/lists/{id}/items/{itemId}/duplicate", 2, true),
            new Op(HttpMethod.POST, "/v1/lists/{id}/items:import", 1, true),
            new Op(HttpMethod.GET, "/v1/receipts", 0, false),
            new Op(HttpMethod.GET, "/v1/receipts/{id}", 1, false),
            new Op(HttpMethod.POST, "/v1/receipts/{id}/shop-again", 1, true),
            new Op(HttpMethod.GET, "/v1/me", 0, false),
            new Op(HttpMethod.PUT, "/v1/me", 0, true),
            new Op(HttpMethod.PATCH, "/v1/me/preferences", 0, true));

    @BeforeEach
    void signedIn() {
        signIn("Hostile");
    }

    static Stream<Op> operationsWithABody() {
        return OPERATIONS.stream().filter(Op::hasBody);
    }

    static Stream<Op> operationsWithAPathVariable() {
        return OPERATIONS.stream().filter(op -> op.pathVars() > 0);
    }

    @ParameterizedTest
    @MethodSource("operationsWithABody")
    void a_malformed_body_is_a_clean_bad_request(Op op) {
        Object[] ids = new UUID[op.pathVars()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = UUID.randomUUID();
        }
        MvcTestResult result = http.method(op.method()).uri(op.uri(), ids)
                .contentType(MediaType.APPLICATION_JSON).content("{\"broken\": ").exchange();

        assertThat(result).as(op.toString()).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("MALFORMED_REQUEST");
        assertNoLeak(op.toString(), result);
    }

    @ParameterizedTest
    @MethodSource("operationsWithAPathVariable")
    void a_hostile_path_variable_is_a_clean_bad_request(Op op) {
        Object[] ids = new String[op.pathVars()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = HOSTILE_PATH_SEGMENT;
        }
        MvcTestResult result = http.method(op.method()).uri(op.uri(), ids).exchange();

        assertThat(result).as(op.toString()).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("MALFORMED_REQUEST");
        assertNoLeak(op.toString(), result);
    }

    @Test
    void a_malformed_query_parameter_is_a_clean_validation_error() {
        MvcTestResult result = http.get().uri("/v1/receipts/summary?from=not-a-date&to=not-a-date").exchange();

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_FAILED");
        assertNoLeak("summary", result);
    }

    @Test
    void an_unsupported_content_type_never_leaks_internals() {
        MvcTestResult result = http.post().uri("/v1/lists").contentType(MediaType.TEXT_PLAIN).content("hello")
                .exchange();

        assertThat(result.getResponse().getStatus()).isGreaterThanOrEqualTo(400);
        assertNoLeak("unsupported content type", result);
    }

    @Test
    void anything_unmapped_becomes_a_bare_internal_error_with_no_detail() {
        RequestCorrelation.start("req_test");
        try {
            ResponseEntity<ProblemDetail> response =
                    new ApiExceptionHandler().internal(new RuntimeException("SELECT secret FROM accounts"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            ProblemDetail body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getProperties()).containsEntry("code", "INTERNAL").containsEntry("message",
                    "Something went wrong.");
            assertNoLeak("catch-all", body.getProperties().toString());
        } finally {
            RequestCorrelation.clear();
        }
    }

    @Test
    void sql_and_script_shaped_text_is_stored_and_returned_verbatim_not_leaked_or_executed() {
        MvcTestResult created = http.post().uri("/v1/lists").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"<script>alert(1)</script>\"}").exchange();
        assertThat(created).hasStatus(HttpStatus.CREATED).bodyJson()
                .extractingPath("$.name").isEqualTo("<script>alert(1)</script>");

        var list = client(ListsApi.class).create(new CreateListRequest(null, SQLI, null, null)).getBody();
        assertThat(list.name()).isEqualTo(SQLI);
    }

    private static void assertNoLeak(String context, MvcTestResult result) {
        assertNoLeak(context, new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8));
    }

    private static void assertNoLeak(String context, String body) {
        for (String leak : LEAKS) {
            assertThat(body).as(context).doesNotContain(leak);
        }
    }
}
