package app.meucarrinho.api.rest.lists;

import java.util.List;

/** Wrapped in an object so fields can be added later without breaking clients (spec §13). */
public record ListCardsResponse(List<ListCardResponse> lists) {}
