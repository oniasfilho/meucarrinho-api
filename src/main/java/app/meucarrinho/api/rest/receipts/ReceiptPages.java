package app.meucarrinho.api.rest.receipts;

import app.meucarrinho.api.rest.ApiProblem;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Paging for {@code GET /v1/receipts} (ADR 0008). A cursor is opaque to clients and names the newest month of the
 * next page; each page covers {@value #MONTHS_PER_PAGE} months going back. Paging stops at {@link #FIRST_MONTH}:
 * no receipt can be older than the product, so the client never pages through empty years.
 */
final class ReceiptPages {
    static final int MONTHS_PER_PAGE = 6;
    static final YearMonth FIRST_MONTH = YearMonth.of(2026, 1);
    private static final String VERSION = "m1:";

    record Window(YearMonth from, YearMonth to, @Nullable String nextCursor) {}

    private ReceiptPages() {}

    /** The months to read for this cursor, or empty when the cursor already points before the first month. */
    static Optional<Window> window(@Nullable String cursor, YearMonth currentMonth) {
        YearMonth to = cursor == null ? currentMonth : decode(cursor);
        if (to.isBefore(FIRST_MONTH)) {
            return Optional.empty();
        }
        YearMonth earliest = to.minusMonths(MONTHS_PER_PAGE - 1);
        YearMonth from = earliest.isBefore(FIRST_MONTH) ? FIRST_MONTH : earliest;
        String next = from.isAfter(FIRST_MONTH) ? encode(from.minusMonths(1)) : null;
        return Optional.of(new Window(from, to, next));
    }

    static String encode(YearMonth start) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((VERSION + start).getBytes(StandardCharsets.UTF_8));
    }

    static YearMonth decode(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            if (!raw.startsWith(VERSION)) {
                throw ApiProblem.malformed("The cursor is not valid.");
            }
            return YearMonth.parse(raw.substring(VERSION.length()));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw ApiProblem.malformed("The cursor is not valid.");
        }
    }
}
