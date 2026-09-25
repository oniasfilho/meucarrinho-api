package app.meucarrinho.application.sync.port;

import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ListId;
import java.time.Instant;

public record LoggedOp(ListId listId, long seq, SyncOp op, ActorRef actor, Instant appliedAt) {}
