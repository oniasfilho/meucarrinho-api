package br.com.oniasfilho.meucarrinho.session.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import br.com.oniasfilho.meucarrinho.session.ShoppingSessionStatus;

public record ShoppingSessionSummaryResponse(
    UUID id,
    String name,
    String storeName,
    BigDecimal budget,
    ShoppingSessionStatus status,
    int itemCount,
    BigDecimal total,
    BigDecimal remainingBudget,
    boolean overBudget,
    Instant createdAt,
    Instant completedAt
) {
}

