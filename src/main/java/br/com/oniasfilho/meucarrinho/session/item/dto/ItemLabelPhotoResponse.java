package br.com.oniasfilho.meucarrinho.session.item.dto;

import java.time.Instant;

public record ItemLabelPhotoResponse(
    String labelPhotoKey,
    String labelPhotoUrl,
    Instant urlExpiresAt
) {
}
