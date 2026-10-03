package app.meucarrinho.api.rest.receipts;

import static app.meucarrinho.api.rest.ApiErrors.unwrap;

import app.meucarrinho.api.rest.ApiErrors;
import app.meucarrinho.api.rest.ApiProblem;
import app.meucarrinho.api.rest.CurrentActor;
import app.meucarrinho.application.receipts.GetReceipt;
import app.meucarrinho.application.receipts.GetReceiptHistory;
import app.meucarrinho.domain.shared.AccountId;
import java.time.InstantSource;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.RestController;

/** Maps HTTP to the receipt input ports and back; no rules live here (spec §3). */
@RestController
class ReceiptsController implements ReceiptsApi {
    private final CurrentActor caller;
    private final GetReceipt getReceipt;
    private final GetReceiptHistory history;
    private final InstantSource clock;

    ReceiptsController(CurrentActor caller, GetReceipt getReceipt, GetReceiptHistory history, InstantSource clock) {
        this.caller = caller;
        this.getReceipt = getReceipt;
        this.history = history;
        this.clock = clock;
    }

    @Override
    public ReceiptPageResponse history(@Nullable String cursor) {
        AccountId me = caller.require();
        YearMonth current = YearMonth.from(clock.instant().atZone(GetReceiptHistory.DEFAULT_ZONE));
        return ReceiptPages.window(cursor, current)
                .map(window -> new ReceiptPageResponse(ReceiptMapper.months(
                        history.history(me, window.from(), window.to(), GetReceiptHistory.DEFAULT_ZONE)),
                        window.nextCursor()))
                .orElseGet(() -> new ReceiptPageResponse(List.of(), null));
    }

    @Override
    public MonthTotalsResponse summary(String from, String to) {
        AccountId me = caller.require();
        YearMonth start = month("from", from);
        YearMonth end = month("to", to);
        if (start.isAfter(end)) {
            throw ApiProblem.invalid("from", "AFTER_TO", "The first month must not be after the last.");
        }
        return new MonthTotalsResponse(ReceiptMapper.totals(
                history.history(me, start, end, GetReceiptHistory.DEFAULT_ZONE)));
    }

    @Override
    public ReceiptResponse get(UUID id) {
        AccountId me = caller.require();
        return ReceiptMapper.response(unwrap(getReceipt.get(ReceiptMapper.receiptId(id), me), ApiErrors::of));
    }

    private static YearMonth month(String field, String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiProblem.invalid(field, "INVALID_MONTH", "Write months as yyyy-MM.");
        }
    }
}
