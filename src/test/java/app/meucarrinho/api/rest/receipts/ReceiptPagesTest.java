package app.meucarrinho.api.rest.receipts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.meucarrinho.api.rest.ApiProblem;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class ReceiptPagesTest {
    private static final YearMonth OCTOBER = YearMonth.of(2026, 10);

    @Test
    void the_first_page_covers_six_months_up_to_this_one() {
        ReceiptPages.Window first = ReceiptPages.window(null, OCTOBER).orElseThrow();

        assertThat(first.from()).isEqualTo(YearMonth.of(2026, 5));
        assertThat(first.to()).isEqualTo(OCTOBER);
        assertThat(first.nextCursor()).isNotNull();
        assertThat(ReceiptPages.decode(first.nextCursor())).isEqualTo(YearMonth.of(2026, 4));
    }

    @Test
    void paging_stops_at_the_first_month_of_the_product() {
        String cursor = ReceiptPages.window(null, OCTOBER).orElseThrow().nextCursor();

        ReceiptPages.Window last = ReceiptPages.window(cursor, OCTOBER).orElseThrow();

        assertThat(last.from()).isEqualTo(ReceiptPages.FIRST_MONTH);
        assertThat(last.to()).isEqualTo(YearMonth.of(2026, 4));
        assertThat(last.nextCursor()).isNull();
        assertThat(ReceiptPages.window(ReceiptPages.encode(YearMonth.of(2025, 12)), OCTOBER)).isEmpty();
    }

    @Test
    void cursors_are_opaque_and_checked() {
        assertThat(ReceiptPages.encode(OCTOBER)).doesNotContain("2026");
        for (String bad : new String[] {"2026-10", "!!!", ReceiptPages.encode(OCTOBER).substring(2)}) {
            assertThatThrownBy(() -> ReceiptPages.decode(bad)).as(bad).isInstanceOfSatisfying(ApiProblem.class,
                    problem -> assertThat(problem.code()).isEqualTo("MALFORMED_REQUEST"));
        }
    }
}
