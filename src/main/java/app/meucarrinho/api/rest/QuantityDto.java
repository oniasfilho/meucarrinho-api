package app.meucarrinho.api.rest;

import app.meucarrinho.domain.shared.Quantity;
import app.meucarrinho.domain.shared.Unit;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public record QuantityDto(@NotNull @Nullable BigDecimal amount, @NotNull @Nullable Unit unit) {
    public static QuantityDto of(Quantity quantity) {
        return new QuantityDto(quantity.amount().stripTrailingZeros(), quantity.unit());
    }

    public Quantity toQuantity(String field) {
        BigDecimal value = Fields.required(field + ".amount", amount);
        Unit of = Fields.required(field + ".unit", unit);
        return Fields.parse(field, () -> new Quantity(value, of));
    }
}
