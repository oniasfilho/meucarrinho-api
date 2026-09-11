package br.com.oniasfilho.meucarrinho.session.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateShoppingSessionRequest(
    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    @Size(max = 120)
    String name,

    @Size(max = 160)
    String storeName,

    @PositiveOrZero
    @Digits(integer = 10, fraction = 2)
    BigDecimal budget
) {
}

