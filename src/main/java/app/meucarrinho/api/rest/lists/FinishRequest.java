package app.meucarrinho.api.rest.lists;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** {@code receiptId} is the client's UUIDv7 for the receipt; a retry with the same ID returns the same receipt. */
public record FinishRequest(@Nullable UUID receiptId) {}
