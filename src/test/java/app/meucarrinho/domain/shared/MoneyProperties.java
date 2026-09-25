package app.meucarrinho.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.LongRange;

class MoneyProperties {
    private static final long LIMIT = 1_000_000_000_00L;

    @Property
    void addition_is_commutative_and_associative(
            @ForAll @LongRange(min = -LIMIT, max = LIMIT) long a,
            @ForAll @LongRange(min = -LIMIT, max = LIMIT) long b,
            @ForAll @LongRange(min = -LIMIT, max = LIMIT) long c) {
        Money x = Money.brl(a), y = Money.brl(b), z = Money.brl(c);
        assertThat(x.plus(y)).isEqualTo(y.plus(x));
        assertThat(x.plus(y).plus(z)).isEqualTo(x.plus(y.plus(z)));
    }

    @Property
    void minus_undoes_plus(
            @ForAll @LongRange(min = -LIMIT, max = LIMIT) long a,
            @ForAll @LongRange(min = -LIMIT, max = LIMIT) long b) {
        assertThat(Money.brl(a).plus(Money.brl(b)).minus(Money.brl(b))).isEqualTo(Money.brl(a));
    }

    @Property
    void one_unit_costs_the_unit_price(@ForAll @LongRange(min = 0, max = LIMIT) long price) {
        assertThat(Money.brl(price).times(Quantity.one())).isEqualTo(Money.brl(price));
    }

    @Property
    void whole_units_multiply_exactly(
            @ForAll @LongRange(min = 0, max = 1_000_000) long price,
            @ForAll @LongRange(min = 1, max = 1_000) long count) {
        assertThat(Money.brl(price).times(Quantity.units(count)).minorUnits()).isEqualTo(price * count);
    }

    @Property
    void weighed_subtotals_round_to_the_nearest_centavo(
            @ForAll @LongRange(min = 0, max = 1_000_000) long pricePerKg,
            @ForAll("kilograms") Quantity quantity) {
        BigDecimal exact = BigDecimal.valueOf(pricePerKg).multiply(quantity.amount());
        long rounded = Money.brl(pricePerKg).times(quantity).minorUnits();
        assertThat(BigDecimal.valueOf(rounded).subtract(exact).abs()).isLessThanOrEqualTo(new BigDecimal("0.5"));
    }

    @Property
    void comparison_agrees_with_minor_units(@ForAll long a, @ForAll long b) {
        assertThat(Integer.signum(Money.brl(a).compareTo(Money.brl(b)))).isEqualTo(Long.signum(Long.compare(a, b)));
    }

    @Property
    void overflow_fails_loudly(@ForAll @LongRange(min = 1, max = Long.MAX_VALUE) long a) {
        assertThatThrownBy(() -> Money.brl(Long.MAX_VALUE).plus(Money.brl(a))).isInstanceOf(ArithmeticException.class);
    }

    @Provide
    Arbitrary<Quantity> kilograms() {
        return Arbitraries.integers().between(1, 100_000).map(tenths -> new Quantity(BigDecimal.valueOf(tenths, 1), Unit.KG));
    }
}
