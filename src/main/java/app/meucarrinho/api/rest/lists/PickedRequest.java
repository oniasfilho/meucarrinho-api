package app.meucarrinho.api.rest.lists;

import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

public record PickedRequest(@NotNull @Nullable Boolean picked) {}
