package app.meucarrinho.api.rest;

import app.meucarrinho.domain.shared.CurrencyCode;
import app.meucarrinho.domain.shared.Money;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Money on the wire: integer minor units plus the currency, never a decimal (spec §4). */
public record MoneyDto(@NotNull @Nullable Long minorUnits, @NotNull @Nullable CurrencyCode currency) {
    public static MoneyDto of(Money money) {
        return new MoneyDto(money.minorUnits(), money.currency());
    }

    public static @Nullable MoneyDto ofNullable(Optional<Money> money) {
        return money.map(MoneyDto::of).orElse(null);
    }

    public Money toMoney(String field) {
        return new Money(Fields.required(field + ".minorUnits", minorUnits),
                Fields.required(field + ".currency", currency));
    }
}
