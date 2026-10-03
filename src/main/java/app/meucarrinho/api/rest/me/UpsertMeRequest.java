package app.meucarrinho.api.rest.me;

import org.jspecify.annotations.Nullable;

/** Sent right after sign-in; the identity comes from the token, never from the body. */
public record UpsertMeRequest(@Nullable String displayName, @Nullable String email) {}
