package app.meucarrinho.api.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** The step 3b checkpoint: /swagger-ui lists every endpoint built so far (spec §14). */
class OpenApiTest extends RestTest {
    @Test
    void the_openapi_document_lists_every_endpoint_built_so_far() {
        assertThat(http.get().uri("/v3/api-docs")).hasStatusOk().bodyJson()
                .extractingPath("$.paths").asMap().containsOnlyKeys(
                        "/v1/me", "/v1/me/preferences",
                        "/v1/lists", "/v1/lists/{id}", "/v1/lists/{id}/restore", "/v1/lists/{id}/duplicate",
                        "/v1/lists/{id}/finish", "/v1/lists/{id}/items", "/v1/lists/{id}/items/{itemId}",
                        "/v1/lists/{id}/items/{itemId}/picked", "/v1/lists/{id}/items/{itemId}/restore",
                        "/v1/lists/{id}/items/{itemId}/duplicate", "/v1/lists/{id}/items:import",
                        "/v1/receipts", "/v1/receipts/summary", "/v1/receipts/{id}", "/v1/receipts/{id}/shop-again");
    }

    @Test
    void swagger_ui_is_served_at_the_path_in_the_quick_start() {
        assertThat(http.get().uri("/swagger-ui")).hasStatus3xxRedirection()
                .hasRedirectedUrl("/swagger-ui/index.html");
        assertThat(http.get().uri("/swagger-ui/index.html")).hasStatus(HttpStatus.OK);
    }
}
