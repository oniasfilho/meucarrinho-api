package app.meucarrinho.domain.shared;

import java.util.Objects;

public record Budget(Money amount) {
    public Budget {
        Objects.requireNonNull(amount, "amount");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("A budget must be greater than zero: " + amount);
        }
    }

    public static Budget brl(long centavos) {
        return new Budget(Money.brl(centavos));
    }
}
