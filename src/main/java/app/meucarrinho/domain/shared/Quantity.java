package app.meucarrinho.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Quantity(BigDecimal amount, Unit unit) {
    private static final BigDecimal TENTH = new BigDecimal("0.1");

    public Quantity {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(unit, "unit");
        try {
            amount = amount.setScale(3, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Quantity has more than 3 decimals: " + amount, e);
        }
        switch (unit) {
            case UN -> {
                if (amount.compareTo(BigDecimal.ONE) < 0 || amount.stripTrailingZeros().scale() > 0) {
                    throw new IllegalArgumentException("A quantity in UN must be a whole number >= 1: " + amount);
                }
            }
            case KG -> {
                if (amount.compareTo(TENTH) < 0 || amount.remainder(TENTH).signum() != 0) {
                    throw new IllegalArgumentException("A quantity in KG must be >= 0.1 in steps of 0.1: " + amount);
                }
            }
        }
    }

    public static Quantity units(long count) {
        return new Quantity(BigDecimal.valueOf(count), Unit.UN);
    }

    public static Quantity kilograms(String amount) {
        return new Quantity(new BigDecimal(amount), Unit.KG);
    }

    public static Quantity one() {
        return units(1);
    }

    public static boolean isValid(BigDecimal amount, Unit unit) {
        try {
            new Quantity(amount, unit);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return amount.stripTrailingZeros().toPlainString() + " " + unit;
    }
}
