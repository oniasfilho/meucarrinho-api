package app.meucarrinho.api.rest.lists;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.api.rest.CopyRequest;
import app.meucarrinho.api.rest.MoneyDto;
import app.meucarrinho.api.rest.Patch;
import app.meucarrinho.api.rest.QuantityDto;
import app.meucarrinho.api.rest.RestTest;
import app.meucarrinho.api.rest.receipts.ReceiptResponse;
import app.meucarrinho.domain.list.ListStatus;
import app.meucarrinho.domain.shared.CurrencyCode;
import app.meucarrinho.domain.shared.Unit;
import app.meucarrinho.testfixtures.TestIds;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class ItemsApiTest extends RestTest {
    private ListsApi lists;
    private ItemsApi items;
    private UUID listId;

    @BeforeEach
    void aList() {
        lists = client(ListsApi.class);
        items = client(ItemsApi.class);
        signIn("Marina");
        listId = lists.create(new CreateListRequest(null, "Feira", null, null)).getBody().id();
    }

    private static MoneyDto brl(long centavos) {
        return new MoneyDto(centavos, CurrencyCode.BRL);
    }

    private static ItemResponse only(ResponseEntity<ListResponse> response) {
        assertThat(response.getBody().items()).hasSize(1);
        return response.getBody().items().getFirst();
    }

    @Test
    void the_quick_add_bar_and_the_editor_both_add_items() {
        ResponseEntity<ListResponse> quick = items.add(listId, "\"1\"", AddItemRequest.quickAdd(null, "2 leite 5,49"));
        assertThat(only(quick)).satisfies(item -> {
            assertThat(item.name()).isEqualTo("leite");
            assertThat(item.quantity()).isEqualTo(new QuantityDto(new BigDecimal("2"), Unit.UN));
            assertThat(item.subtotal()).isEqualTo(brl(10_98));
        });

        ResponseEntity<ListResponse> edited = items.add(listId, quick.getHeaders().getETag(), new AddItemRequest(null,
                null, "tomate", new QuantityDto(new BigDecimal("1.2"), Unit.KG), brl(8_90), "maduros"));

        assertThat(edited.getHeaders().getETag()).isEqualTo("\"3\"");
        assertThat(edited.getBody().items()).extracting(ItemResponse::name).containsExactly("leite", "tomate");
        assertThat(edited.getBody().totals().estimatedTotal()).isEqualTo(brl(10_98 + 10_68));
    }

    @Test
    void an_add_takes_text_or_fields_and_text_must_be_an_item() {
        String both = "{\"text\":\"cafe\",\"name\":\"cafe\"}";
        assertThat(http.post().uri("/v1/lists/{id}/items", listId).header("If-Match", "1")
                .contentType(MediaType.APPLICATION_JSON).content(both))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.errors[0].code").isEqualTo("TEXT_OR_FIELDS");
        assertThat(http.post().uri("/v1/lists/{id}/items", listId).header("If-Match", "1")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.errors[0].field").isEqualTo("name");
        assertThat(http.post().uri("/v1/lists/{id}/items", listId).header("If-Match", "1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"1,5 leite\"}"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.errors[0].code").isEqualTo("INVALID_QUANTITY");
        assertThat(http.post().uri("/v1/lists/{id}/items", listId).header("If-Match", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"cafe\",\"unitPrice\":{\"minorUnits\":-5,\"currency\":\"BRL\"}}"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.errors[0].field").isEqualTo("unitPrice");
    }

    @Test
    void an_edit_keeps_absent_fields_and_clears_null_ones() {
        ResponseEntity<ListResponse> added = items.add(listId, "1", new AddItemRequest(null, null, "cafe", null,
                brl(18_90), "o de sempre"));
        UUID itemId = only(added).id();

        ResponseEntity<ListResponse> kept = items.edit(listId, itemId, added.getHeaders().getETag(),
                new EditItemRequest("café", null, Patch.keep(), Patch.keep(), Patch.keep()));
        assertThat(only(kept)).satisfies(item -> {
            assertThat(item.name()).isEqualTo("café");
            assertThat(item.unitPrice()).isEqualTo(brl(18_90));
            assertThat(item.note()).isEqualTo("o de sempre");
        });

        assertThat(http.patch().uri("/v1/lists/{id}/items/{itemId}", listId, itemId)
                .header("If-Match", kept.getHeaders().getETag()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"unitPrice\":null,\"note\":null}"))
                .hasStatusOk()
                .bodyJson().satisfies(body -> {
                    assertThat(body).extractingPath("$.items[0].unitPrice").isNull();
                    assertThat(body).extractingPath("$.items[0].note").isNull();
                    assertThat(body).extractingPath("$.totals.unpricedCount").isEqualTo(1);
                });
    }

    @Test
    void items_are_picked_returned_removed_restored_and_duplicated() {
        ResponseEntity<ListResponse> list = items.add(listId, "1", AddItemRequest.quickAdd(null, "cafe 18,90"));
        UUID cafe = only(list).id();

        list = items.setPicked(listId, cafe, list.getHeaders().getETag(), new PickedRequest(true));
        assertThat(only(list).picked()).isTrue();
        assertThat(only(list).pickedBy().id()).isEqualTo(actor.account().orElseThrow().value());
        list = items.setPicked(listId, cafe, list.getHeaders().getETag(), new PickedRequest(false));
        assertThat(only(list).picked()).isFalse();

        list = items.remove(listId, cafe, list.getHeaders().getETag());
        assertThat(list.getBody().items()).isEmpty();
        list = items.restore(listId, cafe, list.getHeaders().getETag());
        assertThat(only(list).id()).isEqualTo(cafe);

        UUID copy = TestIds.itemId().value();
        list = items.duplicate(listId, cafe, list.getHeaders().getETag(), new CopyRequest(copy));
        assertThat(list.getBody().items()).extracting(ItemResponse::id).containsExactly(cafe, copy);
    }

    @Test
    void a_retried_add_with_the_same_item_id_answers_with_the_list_as_it_is() {
        UUID itemId = TestIds.itemId().value();

        ResponseEntity<ListResponse> first = items.add(listId, "1", AddItemRequest.quickAdd(itemId, "leite"));
        ResponseEntity<ListResponse> retry = items.add(listId, "1", AddItemRequest.quickAdd(itemId, "leite"));

        assertThat(retry.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retry.getHeaders().getETag()).isEqualTo(first.getHeaders().getETag());
        assertThat(only(retry).id()).isEqualTo(itemId);
    }

    @Test
    void finishing_makes_a_receipt_whose_items_a_new_list_can_import() {
        assertThat(http.post().uri("/v1/lists/{id}/finish", listId).header("If-Match", "1"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("NOTHING_PICKED");
        ResponseEntity<ListResponse> list = items.add(listId, "1", AddItemRequest.quickAdd(null, "2 leite 5,49"));
        list = items.setPicked(listId, only(list).id(), list.getHeaders().getETag(), new PickedRequest(true));

        ResponseEntity<ReceiptResponse> finished = lists.finish(listId, list.getHeaders().getETag(), null);

        assertThat(finished.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(finished.getHeaders().getLocation()).hasToString("/v1/receipts/" + finished.getBody().id());
        assertThat(finished.getBody().total()).isEqualTo(brl(10_98));
        assertThat(lists.get(listId).getBody().status()).isEqualTo(ListStatus.COMPLETED);

        UUID next = lists.create(new CreateListRequest(null, "Feira", null, null)).getBody().id();
        ResponseEntity<ListResponse> imported = items.importItems(next, "1", null);
        assertThat(only(imported)).satisfies(item -> {
            assertThat(item.name()).isEqualTo("leite");
            assertThat(item.picked()).isFalse();
        });
    }
}
