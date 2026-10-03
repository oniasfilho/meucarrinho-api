package app.meucarrinho.api.rest.receipts;

import app.meucarrinho.api.rest.ActorDto;
import app.meucarrinho.api.rest.MoneyDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record ReceiptResponse(
        UUID id,
        UUID listId,
        String name,
        @Nullable String store,
        Instant completedAt,
        ActorDto finishedBy,
        List<ActorDto> participants,
        List<ReceiptLineResponse> lines,
        MoneyDto total,
        @Nullable MoneyDto budget,
        @Nullable MoneyDto budgetDelta,
        boolean overBudget) {}
