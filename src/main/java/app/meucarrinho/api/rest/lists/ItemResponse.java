package app.meucarrinho.api.rest.lists;

import app.meucarrinho.api.rest.ActorDto;
import app.meucarrinho.api.rest.MoneyDto;
import app.meucarrinho.api.rest.QuantityDto;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record ItemResponse(
        UUID id,
        String name,
        QuantityDto quantity,
        @Nullable MoneyDto unitPrice,
        @Nullable MoneyDto subtotal,
        @Nullable String note,
        @Nullable String photoRef,
        boolean picked,
        @Nullable ActorDto pickedBy,
        ActorDto lastEditedBy,
        int position) {}
