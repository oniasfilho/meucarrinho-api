package app.meucarrinho.api.rest.receipts;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.api.rest.CopyRequest;
import app.meucarrinho.api.rest.MoneyDto;
import app.meucarrinho.api.rest.RestTest;
import app.meucarrinho.api.rest.lists.AddItemRequest;
import app.meucarrinho.api.rest.lists.CreateListRequest;
import app.meucarrinho.api.rest.lists.ItemResponse;
import app.meucarrinho.api.rest.lists.ItemsApi;
import app.meucarrinho.api.rest.lists.ListResponse;
import app.meucarrinho.api.rest.lists.ListsApi;
import app.meucarrinho.api.rest.lists.PickedRequest;
import app.meucarrinho.api.rest.lists.ShopAgainApi;
import app.meucarrinho.application.receipts.GetReceiptHistory;
import app.meucarrinho.domain.list.ListStatus;
import app.meucarrinho.domain.shared.CurrencyCode;
import app.meucarrinho.testfixtures.TestIds;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ReceiptsApiTest extends RestTest {
    private ReceiptsApi receipts;
    private ReceiptResponse receipt;

    @BeforeEach
    void aFinishedTrip() {
        receipts = client(ReceiptsApi.class);
        ListsApi lists = client(ListsApi.class);
        ItemsApi items = client(ItemsApi.class);
        signIn("Marina");
        UUID listId = lists.create(new CreateListRequest(null, "Compras da semana", "Mercado Dia", null))
                .getBody().id();
        ResponseEntity<ListResponse> list = items.add(listId, "1", AddItemRequest.quickAdd(null, "cafe 18,90"));
        list = items.setPicked(listId, list.getBody().items().getFirst().id(), list.getHeaders().getETag(),
                new PickedRequest(true));
        receipt = lists.finish(listId, list.getHeaders().getETag(), null).getBody();
    }

    @Test
    void history_pages_back_from_this_month_until_the_cursor_runs_out() {
        List<ReceiptPageResponse.Month> months = new ArrayList<>();
        ReceiptPageResponse page = receipts.history(null);
        months.addAll(page.months());
        for (int pages = 1; page.nextCursor() != null; pages++) {
            assertThat(pages).as("paging ends").isLessThan(100);
            page = receipts.history(page.nextCursor());
            months.addAll(page.months());
        }

        String thisMonth = YearMonth.now(GetReceiptHistory.DEFAULT_ZONE).toString();
        assertThat(months).singleElement().satisfies(month -> {
            assertThat(month.month()).isEqualTo(thisMonth);
            assertThat(month.total()).isEqualTo(new MoneyDto(18_90L, CurrencyCode.BRL));
            assertThat(month.receipts()).singleElement().satisfies(summary -> {
                assertThat(summary.id()).isEqualTo(receipt.id());
                assertThat(summary.lineCount()).isEqualTo(1);
            });
        });
        assertThat(http.get().uri("/v1/receipts?cursor=not-a-cursor")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void the_summary_totals_each_month_in_the_range() {
        String thisMonth = YearMonth.now(GetReceiptHistory.DEFAULT_ZONE).toString();

        assertThat(receipts.summary("2026-01", thisMonth).months()).singleElement().satisfies(month -> {
            assertThat(month.month()).isEqualTo(thisMonth);
            assertThat(month.total()).isEqualTo(new MoneyDto(18_90L, CurrencyCode.BRL));
            assertThat(month.receiptCount()).isEqualTo(1);
        });
        assertThat(http.get().uri("/v1/receipts/summary?from=2026-13&to=2026-12"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.errors[0].field").isEqualTo("from");
        assertThat(http.get().uri("/v1/receipts/summary?from=2026-09&to=2026-08"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.errors[0].code").isEqualTo("AFTER_TO");
        assertThat(http.get().uri("/v1/receipts/summary?from=2026-09")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void a_receipt_is_shown_to_its_participants_only() {
        assertThat(receipts.get(receipt.id())).isEqualTo(receipt);

        signIn("Jessica");
        assertThat(http.get().uri("/v1/receipts/{id}", receipt.id())).hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.code").isEqualTo("RECEIPT_NOT_FOUND");
    }

    @Test
    void shopping_again_makes_a_new_active_list_from_the_receipt() {
        UUID newList = TestIds.listId().value();

        ResponseEntity<ListResponse> again = client(ShopAgainApi.class).shopAgain(receipt.id(), new CopyRequest(newList));

        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(again.getHeaders().getLocation()).hasToString("/v1/lists/" + newList);
        assertThat(again.getBody().status()).isEqualTo(ListStatus.ACTIVE);
        assertThat(again.getBody().items()).extracting(ItemResponse::name).containsExactly("cafe");
        assertThat(again.getBody().items()).noneMatch(ItemResponse::picked);
    }
}
