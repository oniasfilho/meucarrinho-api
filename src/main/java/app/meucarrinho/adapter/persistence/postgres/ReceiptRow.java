package app.meucarrinho.adapter.persistence.postgres;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("receipts")
record ReceiptRow(
        @Id UUID id,
        UUID listId,
        String listName,
        String storeName,
        Instant completedAt,
        String finishedByKind,
        UUID finishedById,
        long totalMinorUnits,
        Long budgetMinorUnits,
        Long budgetDeltaMinorUnits,
        String currency) {}
