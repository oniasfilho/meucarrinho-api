package app.meucarrinho.api.rest.lists;

import app.meucarrinho.api.rest.CopyRequest;
import app.meucarrinho.api.rest.receipts.ReceiptResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

/** Shopping lists (spec §7). Responses that carry a list send its version as the ETag; writes need it in If-Match. */
@HttpExchange("/v1/lists")
public interface ListsApi {
    /** Home cards. Only {@code status=active} exists. */
    @GetExchange
    ListCardsResponse activeLists(@RequestParam(name = "status", required = false) @Nullable String status);

    @PostExchange
    ResponseEntity<ListResponse> create(@Valid @RequestBody CreateListRequest request);

    @GetExchange("/{id}")
    ResponseEntity<ListResponse> get(@PathVariable("id") UUID id);

    @PatchExchange("/{id}")
    ResponseEntity<ListResponse> update(@PathVariable("id") UUID id,
            @RequestHeader(name = "If-Match", required = false) @Nullable String ifMatch,
            @Valid @RequestBody UpdateListRequest request);

    @DeleteExchange("/{id}")
    ResponseEntity<ListResponse> delete(@PathVariable("id") UUID id,
            @RequestHeader(name = "If-Match", required = false) @Nullable String ifMatch);

    @PostExchange("/{id}/restore")
    ResponseEntity<ListResponse> restore(@PathVariable("id") UUID id,
            @RequestHeader(name = "If-Match", required = false) @Nullable String ifMatch);

    @PostExchange("/{id}/duplicate")
    ResponseEntity<ListResponse> duplicate(@PathVariable("id") UUID id,
            @RequestBody(required = false) @Nullable CopyRequest request);

    @PostExchange("/{id}/finish")
    ResponseEntity<ReceiptResponse> finish(@PathVariable("id") UUID id,
            @RequestHeader(name = "If-Match", required = false) @Nullable String ifMatch,
            @RequestBody(required = false) @Nullable FinishRequest request);
}
