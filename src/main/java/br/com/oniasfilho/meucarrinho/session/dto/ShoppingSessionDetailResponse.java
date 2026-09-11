package br.com.oniasfilho.meucarrinho.session.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.oniasfilho.meucarrinho.session.ShoppingSessionStatus;
import br.com.oniasfilho.meucarrinho.session.item.dto.ShoppingSessionItemResponse;

public record ShoppingSessionDetailResponse(
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
    Instant completedAt,
    List<ShoppingSessionItemResponse> items
) {
}

