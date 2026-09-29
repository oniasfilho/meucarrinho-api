package app.meucarrinho.adapter.persistence.postgres;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.relational.core.mapping.Table;

@Table("shopping_list_items")
record ShoppingListItemRow(
        UUID itemId,
        UUID listId,
        int itemIndex,
        String name,
        BigDecimal quantity,
        String unit,
        Long unitPriceMinorUnits,
        String currency,
        String note,
        String photoRef,
        boolean picked,
        String pickedByKind,
        UUID pickedById,
        String lastEditedByKind,
        UUID lastEditedById,
        int position,
        Instant removedAt) {}
