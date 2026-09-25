package app.meucarrinho.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.SplittableRandom;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ValueObjectsTest {
    @Test
    void item_names_match_without_accents_or_case() {
        assertThat(new ItemName("Café").normalized()).isEqualTo(new ItemName("cafe").normalized());
        assertThat(new ItemName("  Pão   de  Queijo ").value()).isEqualTo("Pão de Queijo");
        assertThat(new ItemName("MAÇÃ").normalized()).isEqualTo("maca");
    }

    @Test
    void item_names_are_one_to_120_characters() {
        assertThatThrownBy(() -> new ItemName("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThat(new ItemName("a".repeat(120)).value()).hasSize(120);
        assertThatThrownBy(() -> new ItemName("a".repeat(121))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void notes_and_store_names_have_limits() {
        assertThatThrownBy(() -> new ItemNote("x".repeat(281))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StoreName("x".repeat(81))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void a_budget_is_positive() {
        assertThat(Budget.brl(1).amount()).isEqualTo(Money.brl(1));
        assertThatThrownBy(() -> Budget.brl(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Budget.brl(-100)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void share_codes_are_crockford_base32() {
        assertThat(ShareCode.parse("4k7q")).isEqualTo(new ShareCode("4K7Q"));
        assertThat(ShareCode.parse("o1lI")).isEqualTo(new ShareCode("0111"));
        assertThatThrownBy(() -> ShareCode.parse("4K7U")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ShareCode.parse("4K7")).isInstanceOf(IllegalArgumentException.class);
        var random = new SplittableRandom(7);
        for (int i = 0; i < 1_000; i++) {
            assertThat(ShareCode.random(random).value()).hasSize(4);
        }
    }

    @Test
    void ids_are_uuid_v7_and_ordered_by_time() {
        var random = new SplittableRandom(1);
        UUID earlier = Uuid7.generate(1_758_800_000_000L, random);
        UUID later = Uuid7.generate(1_758_800_000_001L, random);
        assertThat(Uuid7.isV7(earlier)).isTrue();
        assertThat(earlier.toString()).isLessThan(later.toString());
        assertThat(new ListId(earlier).value()).isEqualTo(earlier);
        assertThatThrownBy(() -> new ListId(UUID.randomUUID())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emails_are_lower_cased_and_shape_checked() {
        assertThat(new EmailAddress(" Marina@Example.com ").value()).isEqualTo("marina@example.com");
        assertThatThrownBy(() -> new EmailAddress("marina")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void display_names_give_a_first_name() {
        assertThat(new DisplayName("Marina Souza").firstName()).isEqualTo("Marina");
    }

    @Test
    void results_carry_a_value_or_an_error() {
        Result<Integer, String> ok = Result.ok(2);
        Result<Integer, String> err = Result.err("nope");
        assertThat(ok.map(v -> v * 2).orElseThrow()).isEqualTo(4);
        assertThat(err.map(v -> v * 2).errorOrThrow()).isEqualTo("nope");
    }
}
