package app.meucarrinho.adapter.persistence.postgres;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

@Table("shopping_lists")
record ShoppingListRow(
        @Id UUID id,
        UUID ownerId,
        String name,
        String storeName,
        Long budgetMinorUnits,
        String currency,
        String status,
        String statusBeforeDelete,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt,
        Instant deletedAt,
        UUID receiptId,
        @Version Long version) {}
