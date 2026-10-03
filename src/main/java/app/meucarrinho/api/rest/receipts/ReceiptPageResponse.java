package app.meucarrinho.api.rest.receipts;

import app.meucarrinho.api.rest.MoneyDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** One page of history; {@code nextCursor} is null when there is nothing older. */
public record ReceiptPageResponse(List<Month> months, @Nullable String nextCursor) {
    /** {@code month} is {@code yyyy-MM} in America/Sao_Paulo. */
    public record Month(String month, MoneyDto total, List<Summary> receipts) {}

    public record Summary(UUID id, String name, @Nullable String store, Instant completedAt, MoneyDto total,
            int lineCount, boolean overBudget) {}
}
