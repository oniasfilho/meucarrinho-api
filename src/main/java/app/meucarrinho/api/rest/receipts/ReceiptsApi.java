package app.meucarrinho.api.rest.receipts;

import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Finished purchases (spec §7). "Comprar de novo" is a list write and lives in {@code ShopAgainApi}. */
@HttpExchange("/v1/receipts")
public interface ReceiptsApi {
    /**
     * History, newest first, grouped by month. Each page covers a fixed window of months; pass {@code nextCursor}
     * back to get the window before it, until it is null (ADR 0008).
     */
    @GetExchange
    ReceiptPageResponse history(@RequestParam(name = "cursor", required = false) @Nullable String cursor);

    /** Monthly totals, newest first, for months written as {@code yyyy-MM}. */
    @GetExchange("/summary")
    MonthTotalsResponse summary(@RequestParam("from") String from, @RequestParam("to") String to);

    @GetExchange("/{id}")
    ReceiptResponse get(@PathVariable("id") UUID id);
}
