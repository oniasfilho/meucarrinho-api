package app.meucarrinho.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

class QuantityProperties {
    @Property
    void units_accept_exactly_the_whole_numbers_from_one(@ForAll @IntRange(min = -1_000, max = 100_000) int thousandths) {
        BigDecimal amount = BigDecimal.valueOf(thousandths, 3);
        boolean whole = thousandths % 1000 == 0;
        assertThat(Quantity.isValid(amount, Unit.UN)).isEqualTo(whole && thousandths >= 1000);
    }

    @Property
    void kilograms_accept_tenths_from_one_tenth(@ForAll @IntRange(min = -1_000, max = 100_000) int thousandths) {
        BigDecimal amount = BigDecimal.valueOf(thousandths, 3);
        boolean onTheStep = thousandths % 100 == 0;
        assertThat(Quantity.isValid(amount, Unit.KG)).isEqualTo(onTheStep && thousandths >= 100);
    }

    @Property
    void scale_is_always_three(@ForAll @IntRange(min = 1, max = 10_000) int count) {
        assertThat(Quantity.units(count).amount().scale()).isEqualTo(3);
    }

    @Property
    void equal_amounts_are_equal_regardless_of_input_scale(@ForAll @IntRange(min = 1, max = 10_000) int tenths) {
        Quantity a = new Quantity(BigDecimal.valueOf(tenths, 1), Unit.KG);
        Quantity b = new Quantity(BigDecimal.valueOf(tenths * 100L, 3), Unit.KG);
        assertThat(a).isEqualTo(b);
    }

    @Example
    void more_than_three_decimals_is_rejected() {
        assertThat(Quantity.isValid(new BigDecimal("1.0001"), Unit.KG)).isFalse();
    }
}
