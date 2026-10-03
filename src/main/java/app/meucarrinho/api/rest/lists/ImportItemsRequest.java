package app.meucarrinho.api.rest.lists;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record ImportItemsRequest(@Nullable UUID receiptId) {}
