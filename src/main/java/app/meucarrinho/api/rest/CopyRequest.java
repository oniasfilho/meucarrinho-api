package app.meucarrinho.api.rest;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Body of a call that makes something new from something else; {@code id} is the client's UUIDv7 for it. */
public record CopyRequest(@Nullable UUID id) {}
