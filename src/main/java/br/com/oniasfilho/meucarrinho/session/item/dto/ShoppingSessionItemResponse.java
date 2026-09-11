package br.com.oniasfilho.meucarrinho.session.item.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShoppingSessionItemResponse(
    UUID id,
    String name,
    BigDecimal unitPrice,
    int quantity,
    String note,
    String labelPhotoKey,
    String labelPhotoUrl,
    Instant createdAt,
    Instant updatedAt
) {
}

