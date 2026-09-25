package app.meucarrinho.domain.list;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.domain.shared.Money;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

class QuickAddParserTest {
    record Expected(String name, String quantity, String unit, @Nullable Long unitPrice) {}

    record Vector(String input, @Nullable Expected expected, @Nullable String error) {
        @Override
        public String toString() {
            return "\"" + input + "\"";
        }
    }

    record VectorFile(int schemaVersion, String description, List<Vector> vectors) {}

    static Stream<Vector> vectors() {
        File file = new File(System.getProperty("carrinho.contracts.dir", "contracts"), "quick-add-vectors.json");
        return JsonMapper.builder().build().readValue(file, VectorFile.class).vectors().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("vectors")
    void parses_like_the_app(Vector vector) {
        var result = QuickAddParser.parse(vector.input());

        if (vector.error() != null) {
            assertThat(result.errorOrThrow().code()).isEqualTo(vector.error());
            return;
        }
        Expected expected = java.util.Objects.requireNonNull(vector.expected());
        ItemDraft draft = result.orElseThrow();
        assertThat(draft.name().value()).isEqualTo(expected.name());
        assertThat(draft.quantity().amount()).isEqualByComparingTo(new BigDecimal(expected.quantity()));
        assertThat(draft.quantity().unit().name()).isEqualTo(expected.unit());
        assertThat(draft.unitPrice().map(Money::minorUnits).orElse(null)).isEqualTo(expected.unitPrice());
    }
}
