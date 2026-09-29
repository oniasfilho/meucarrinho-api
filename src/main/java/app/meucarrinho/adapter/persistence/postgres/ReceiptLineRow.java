package app.meucarrinho.adapter.persistence.postgres;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.relational.core.mapping.Table;

@Table("receipt_lines")
record ReceiptLineRow(
        UUID receiptId,
        int lineIndex,
        String name,
        BigDecimal quantity,
        String unit,
        Long unitPriceMinorUnits,
        long subtotalMinorUnits,
        String currency) {}
