package app.meucarrinho.application.sync.port;

import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.Result;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface ChangeLog {
    Duration RETENTION = Duration.ofDays(30);

    Result<LoggedOp, ChangeLogError> append(ListId list, SyncOp op, ActorRef actor, Instant appliedAt);

    Result<List<LoggedOp>, ChangeLogError> since(ListId list, long afterSeq);

    long latestSeq(ListId list);

    void purgeAppliedBefore(Instant cutoff);
}
