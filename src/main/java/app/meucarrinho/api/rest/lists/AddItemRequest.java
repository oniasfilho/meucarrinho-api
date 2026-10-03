package app.meucarrinho.api.rest.lists;

import app.meucarrinho.api.rest.MoneyDto;
import app.meucarrinho.api.rest.QuantityDto;
import jakarta.validation.Valid;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Either {@code text} as typed in the quick-add bar ("2 leite 5,49"), or {@code name} with optional quantity
 * (default 1 UN), unit price and note from the editor. {@code id} is the client's UUIDv7 for the item; a retry with
 * the same ID returns the list unchanged.
 */
public record AddItemRequest(
        @Nullable UUID id,
        @Nullable String text,
        @Nullable String name,
        @Valid @Nullable QuantityDto quantity,
        @Valid @Nullable MoneyDto unitPrice,
        @Nullable String note) {
    public static AddItemRequest quickAdd(@Nullable UUID id, String text) {
        return new AddItemRequest(id, text, null, null, null, null);
    }
}
