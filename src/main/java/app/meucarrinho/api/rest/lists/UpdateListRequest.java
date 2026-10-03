package app.meucarrinho.api.rest.lists;

import app.meucarrinho.api.rest.MoneyDto;
import app.meucarrinho.api.rest.Patch;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/** Absent fields stay as they are; {@code store: null} and {@code budget: null} remove them. */
public record UpdateListRequest(
        @Nullable String name,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) Patch<String> store,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) Patch<MoneyDto> budget) {
    public static UpdateListRequest rename(String name) {
        return new UpdateListRequest(name, Patch.keep(), Patch.keep());
    }
}
