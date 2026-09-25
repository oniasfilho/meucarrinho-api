package app.meucarrinho.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(long minorUnits, CurrencyCode currency) implements Comparable<Money> {
    public static final Money ZERO = brl(0);

    public static Money brl(long centavos) {
        return new Money(centavos, CurrencyCode.BRL);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(minorUnits, other.minorUnits), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(minorUnits, other.minorUnits), currency);
    }

    public Money times(Quantity quantity) {
        BigDecimal product = BigDecimal.valueOf(minorUnits).multiply(quantity.amount());
        return new Money(product.setScale(0, RoundingMode.HALF_UP).longValueExact(), currency);
    }

    public boolean isNegative() {
        return minorUnits < 0;
    }

    public boolean isPositive() {
        return minorUnits > 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return Long.compare(minorUnits, other.minorUnits);
    }

    public BigDecimal toDecimal() {
        return BigDecimal.valueOf(minorUnits, currency.minorDigits());
    }

    private void requireSameCurrency(Money other) {
        if (currency != other.currency) {
            throw new IllegalArgumentException("Currency mismatch: " + currency + " vs " + other.currency);
        }
    }

    @Override
    public String toString() {
        return currency + " " + toDecimal().toPlainString();
    }
}
