package br.com.oniasfilho.meucarrinho.user.dto;

import java.math.BigDecimal;

public record AccountStatsResponse(
    long sessionCount,
    long completedSessionCount,
    BigDecimal totalRecordedSpending
) {
}

