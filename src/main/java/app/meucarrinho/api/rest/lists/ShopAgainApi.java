package app.meucarrinho.api.rest.lists;

import app.meucarrinho.api.rest.CopyRequest;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * "Comprar de novo": a new active list from a receipt. The URL sits under receipts, but it creates a list, so it
 * lives with the lists API, as {@code ShopAgain} lives in the lists capability (ADR 0002).
 */
@HttpExchange("/v1/receipts")
public interface ShopAgainApi {
    @PostExchange("/{id}/shop-again")
    ResponseEntity<ListResponse> shopAgain(@PathVariable("id") UUID receiptId,
            @RequestBody(required = false) @Nullable CopyRequest request);
}
