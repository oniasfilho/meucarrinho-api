package br.com.oniasfilho.meucarrinho.session.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateShoppingSessionRequest(
    @NotBlank
    @Size(max = 120)
    String name,

    @Size(max = 160)
    String storeName,

    @PositiveOrZero
    @Digits(integer = 10, fraction = 2)
    BigDecimal budget
) {
}

