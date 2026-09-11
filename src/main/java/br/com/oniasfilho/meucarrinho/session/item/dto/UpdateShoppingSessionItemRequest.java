package br.com.oniasfilho.meucarrinho.session.item.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateShoppingSessionItemRequest(
    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    @Size(max = 160)
    String name,

    @PositiveOrZero
    @Digits(integer = 10, fraction = 2)
    BigDecimal unitPrice,

    @Min(1)
    Integer quantity,

    String note
) {
}

