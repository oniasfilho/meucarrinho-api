package br.com.oniasfilho.meucarrinho.session.item.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateShoppingSessionItemRequest(
    @NotBlank
    @Size(max = 160)
    String name,

    @NotNull
    @PositiveOrZero
    @Digits(integer = 10, fraction = 2)
    BigDecimal unitPrice,

    @NotNull
    @Min(1)
    Integer quantity,

    String note
) {
}

