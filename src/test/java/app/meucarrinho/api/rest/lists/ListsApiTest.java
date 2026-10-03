package app.meucarrinho.api.rest.lists;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.api.rest.CopyRequest;
import app.meucarrinho.api.rest.MoneyDto;
import app.meucarrinho.api.rest.Patch;
import app.meucarrinho.api.rest.RestTest;
import app.meucarrinho.domain.list.ListStatus;
import app.meucarrinho.domain.shared.CurrencyCode;
import app.meucarrinho.testfixtures.TestIds;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class ListsApiTest extends RestTest {
    private ListsApi lists;

    @BeforeEach
    void lists() {
        lists = client(ListsApi.class);
    }

    private ResponseEntity<ListResponse> newList(UUID id) {
        return lists.create(new CreateListRequest(id, "Feira", "Mercado Dia", new MoneyDto(50_00L, CurrencyCode.BRL)));
    }

    @Test
    void a_list_is_created_read_changed_and_shown_on_the_home_cards_with_its_version_as_etag() {
        signIn("Marina");
        UUID id = TestIds.listId().value();

        ResponseEntity<ListResponse> created = newList(id);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).hasToString("/v1/lists/" + id);
        assertThat(created.getHeaders().getETag()).isEqualTo("\"1\"");
        assertThat(created.getBody()).satisfies(list -> {
            assertThat(list.name()).isEqualTo("Feira");
            assertThat(list.status()).isEqualTo(ListStatus.ACTIVE);
            assertThat(list.totals().remainingBudget()).isEqualTo(new MoneyDto(50_00L, CurrencyCode.BRL));
        });

        ResponseEntity<ListResponse> changed = lists.update(id, created.getHeaders().getETag(),
                new UpdateListRequest("Feira grande", Patch.clear(), Patch.keep()));

        assertThat(changed.getHeaders().getETag()).isEqualTo("\"2\"");
        assertThat(changed.getBody()).satisfies(list -> {
            assertThat(list.name()).isEqualTo("Feira grande");
            assertThat(list.store()).isNull();
            assertThat(list.budget()).isEqualTo(new MoneyDto(50_00L, CurrencyCode.BRL));
        });
        assertThat(lists.get(id).getBody().version()).isEqualTo(2);
        assertThat(lists.activeLists("active").lists()).extracting(ListCardResponse::id).containsExactly(id);
        assertThat(lists.activeLists(null).lists()).extracting(ListCardResponse::id).containsExactly(id);
    }

    @Test
    void a_list_write_needs_if_match_and_a_stale_one_is_a_version_conflict() {
        signIn("Marina");
        UUID id = TestIds.listId().value();
        newList(id);
        lists.update(id, "\"1\"", UpdateListRequest.rename("Feira 2"));

        assertThat(http.patch().uri("/v1/lists/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"x\"}"))
                .hasStatus(HttpStatus.PRECONDITION_REQUIRED)
                .bodyJson().extractingPath("$.code").isEqualTo("PRECONDITION_REQUIRED");
        assertThat(http.patch().uri("/v1/lists/{id}", id).header("If-Match", "W/\"2\"")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("MALFORMED_REQUEST");
        assertThat(http.patch().uri("/v1/lists/{id}", id).header("If-Match", "\"1\"")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .hasStatus(HttpStatus.PRECONDITION_FAILED)
                .bodyJson().satisfies(body -> {
                    assertThat(body).extractingPath("$.code").isEqualTo("VERSION_CONFLICT");
                    assertThat(body).extractingPath("$.currentRevision").isEqualTo(2);
                });
        assertThat(http.patch().uri("/v1/lists/{id}", id).header("If-Match", "2")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .hasStatusOk().headers().hasValue("ETag", "\"3\"");
    }

    @Test
    void a_deleted_list_comes_back_with_the_version_from_the_delete() {
        signIn("Marina");
        UUID id = TestIds.listId().value();
        newList(id);

        ResponseEntity<ListResponse> deleted = lists.delete(id, "\"1\"");

        assertThat(deleted.getBody().status()).isEqualTo(ListStatus.DELETED);
        assertThat(http.get().uri("/v1/lists/{id}", id)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(lists.restore(id, deleted.getHeaders().getETag()).getBody().status()).isEqualTo(ListStatus.ACTIVE);
    }

    @Test
    void duplicating_makes_a_new_list_under_the_clients_id() {
        signIn("Marina");
        UUID source = TestIds.listId().value();
        newList(source);
        UUID copy = TestIds.listId().value();

        ResponseEntity<ListResponse> duplicated = lists.duplicate(source, new CopyRequest(copy));

        assertThat(duplicated.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(duplicated.getBody().id()).isEqualTo(copy);
        assertThat(lists.duplicate(source, null).getBody().id()).isNotIn(source, copy);
    }

    @Test
    void strangers_get_not_found_and_anonymous_callers_unauthenticated() {
        signIn("Marina");
        UUID id = TestIds.listId().value();
        newList(id);

        signIn("Jessica");
        assertThat(http.get().uri("/v1/lists/{id}", id)).hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.code").isEqualTo("LIST_NOT_FOUND");

        actor.signOut();
        assertThat(http.get().uri("/v1/lists/{id}", id)).hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.code").isEqualTo("UNAUTHENTICATED");
        assertThat(http.get().uri("/v1/lists?status=active")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void bad_fields_are_named_and_unknown_fields_are_ignored() {
        signIn("Marina");

        assertThat(http.post().uri("/v1/lists").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + "x".repeat(81) + "\"}"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().satisfies(body -> {
                    assertThat(body).extractingPath("$.code").isEqualTo("VALIDATION_FAILED");
                    assertThat(body).extractingPath("$.errors[0].field").isEqualTo("name");
                });
        assertThat(http.post().uri("/v1/lists").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.errors[0].field").isEqualTo("name");
        assertThat(http.post().uri("/v1/lists").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Feira\",\"budget\":{\"minorUnits\":-1,\"currency\":\"BRL\"}}"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.errors[0].field").isEqualTo("budget");
        assertThat(http.post().uri("/v1/lists").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"Feira\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("MALFORMED_REQUEST");
        assertThat(http.post().uri("/v1/lists").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Feira\",\"colour\":\"blue\"}"))
                .hasStatus(HttpStatus.CREATED);
        assertThat(http.get().uri("/v1/lists?status=done")).hasStatus(HttpStatus.BAD_REQUEST);
    }
}
