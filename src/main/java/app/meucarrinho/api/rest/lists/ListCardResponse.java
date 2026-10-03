package app.meucarrinho.api.rest.lists;

import app.meucarrinho.domain.list.ListStatus;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** One home card ("Em andamento"): name, store, totals and how many people share the list. */
public record ListCardResponse(
        UUID id,
        String name,
        @Nullable String store,
        ListStatus status,
        TotalsResponse totals,
        int people,
        Instant updatedAt) {}
