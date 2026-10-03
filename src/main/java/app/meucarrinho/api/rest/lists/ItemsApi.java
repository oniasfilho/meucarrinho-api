package app.meucarrinho.api.rest.lists;

import app.meucarrinho.api.rest.CopyRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

/** Items inside a list (spec §7). Every call is a list write: it needs If-Match and returns the whole list. */
@HttpExchange("/v1/lists/{id}")
public interface ItemsApi {
    /** Quick-add ({@code text}) or editor fields ({@code name} and the rest), never both. */
    @PostExchange("/items")
    ResponseEntity<ListResponse> add(@PathVariable("id") UUID id,
            @RequestHeader(name = "If-Match", required = false) @Nullable String ifMatch,
            @Valid @RequestBody AddItemRequest request);

    @PatchExchange("/items/{itemId}")
    ResponseEntity<ListResponse> edit(@PathVariable("id") UUID id, @PathVariable("itemId") UUID itemId,
            @RequestHeader(name = "If-Match", required = false) @Nullable String ifMatch,
            @Valid @RequestBody EditItemRequest request);

    @PutExchange("/items/{itemId}/picked")
    ResponseEntity<ListResponse> setPicked(@PathVariable("id") UUID id, @PathVariable("itemId") UUID itemId,
            @RequestHeader(name = "If-Match", required = false) @Nullable String ifMatch,
            @Valid @RequestBody PickedRequest request);

    @DeleteExchange("/items/{itemId}")
    ResponseEntity<ListResponse> remove(@PathVariable("id") UUID id, @PathVariable("itemId") UUID itemId,
            @RequestHeader(name = "If-Match", required = false) @Nullable String ifMatch);

    @PostExchange("/items/{itemId}/restore")
    ResponseEntity<ListResponse> restore(@PathVariable("id") UUID id, @PathVariable("itemId") UUID itemId,
            @RequestHeader(name = "If-Match", required = false) @Nullable String ifMatch);

    @PostExchange("/items/{itemId}/duplicate")
    ResponseEntity<ListResponse> duplicate(@PathVariable("id") UUID id, @PathVariable("itemId") UUID itemId,
            @RequestHeader(name = "If-Match", required = false) @Nullable String ifMatch,
            @RequestBody(required = false) @Nullable CopyRequest request);

    /** Copies the items of a receipt into this list; without {@code receiptId}, the latest one. */
    @PostExchange("/items:import")
    ResponseEntity<ListResponse> importItems(@PathVariable("id") UUID id,
            @RequestHeader(name = "If-Match", required = false) @Nullable String ifMatch,
            @RequestBody(required = false) @Nullable ImportItemsRequest request);
}
