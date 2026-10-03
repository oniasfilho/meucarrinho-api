package app.meucarrinho.api.rest.lists;

import app.meucarrinho.api.rest.MoneyDto;
import app.meucarrinho.domain.list.ListStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** A whole list with its live items and the totals computed by the domain (spec §4: clients never sum). */
public record ListResponse(
        UUID id,
        UUID ownerId,
        String name,
        @Nullable String store,
        @Nullable MoneyDto budget,
        ListStatus status,
        long version,
        List<MemberResponse> members,
        List<ItemResponse> items,
        TotalsResponse totals,
        Instant createdAt,
        Instant updatedAt,
        @Nullable Instant completedAt,
        @Nullable Instant deletedAt,
        @Nullable UUID receiptId) {}
