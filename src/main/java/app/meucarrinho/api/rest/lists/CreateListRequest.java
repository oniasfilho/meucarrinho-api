package app.meucarrinho.api.rest.lists;

import app.meucarrinho.api.rest.MoneyDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** {@code id} is the client's UUIDv7 for the new list; a retry with the same ID returns the same list. */
public record CreateListRequest(
        @Nullable UUID id,
        @NotNull @Nullable String name,
        @Nullable String store,
        @Valid @Nullable MoneyDto budget) {}
