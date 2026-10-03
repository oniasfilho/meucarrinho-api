package app.meucarrinho.api.rest.lists;

import app.meucarrinho.api.rest.MoneyDto;
import app.meucarrinho.api.rest.Patch;
import app.meucarrinho.api.rest.QuantityDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

/** Absent fields stay as they are; {@code null} removes a price, note or photo. */
public record EditItemRequest(
        @Nullable String name,
        @Valid @Nullable QuantityDto quantity,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) Patch<MoneyDto> unitPrice,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) Patch<String> note,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) Patch<String> photoRef) {}
