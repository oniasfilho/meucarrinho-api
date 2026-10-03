package app.meucarrinho.api.rest;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.domain.shared.Change;
import app.meucarrinho.domain.shared.CurrencyCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class PatchTest {
    record Body(@JsonInclude(JsonInclude.Include.NON_EMPTY) Patch<MoneyDto> budget) {}

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void absent_keeps_null_clears_and_a_value_sets() {
        assertThat(json.readValue("{}", Body.class).budget()).isEqualTo(Patch.keep());
        assertThat(json.readValue("{\"budget\":null}", Body.class).budget()).isEqualTo(Patch.clear());
        assertThat(json.readValue("{\"budget\":{\"minorUnits\":5000,\"currency\":\"BRL\"}}", Body.class).budget())
                .isEqualTo(Patch.set(new MoneyDto(5000L, CurrencyCode.BRL)));
    }

    @Test
    void a_typed_client_writes_the_same_three_cases() {
        assertThat(json.writeValueAsString(new Body(Patch.keep()))).isEqualTo("{}");
        assertThat(json.writeValueAsString(new Body(Patch.clear()))).isEqualTo("{\"budget\":null}");
        assertThat(json.writeValueAsString(new Body(Patch.set(new MoneyDto(5000L, CurrencyCode.BRL)))))
                .isEqualTo("{\"budget\":{\"minorUnits\":5000,\"currency\":\"BRL\"}}");
    }

    @Test
    void it_becomes_the_domain_change() {
        assertThat(Patch.<String>keep().toChange(String::length)).isEqualTo(Change.keep());
        assertThat(Patch.<String>clear().toChange(String::length)).isEqualTo(Change.clear());
        assertThat(Patch.set("feira").toChange(String::length)).isEqualTo(Change.set(5));
    }
}
