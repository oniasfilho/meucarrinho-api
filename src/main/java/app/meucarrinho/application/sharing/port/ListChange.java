package app.meucarrinho.application.sharing.port;

import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ItemId;
import app.meucarrinho.domain.shared.ListId;
import java.time.Instant;
import java.util.Optional;

public record ListChange(ListId listId, long seq, String op, Optional<ItemId> itemId, ActorRef actor, Instant at) {}
