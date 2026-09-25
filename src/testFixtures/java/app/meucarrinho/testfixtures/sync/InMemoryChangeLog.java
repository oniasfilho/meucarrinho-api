package app.meucarrinho.testfixtures.sync;

import app.meucarrinho.application.sync.port.ChangeLog;
import app.meucarrinho.application.sync.port.ChangeLogError;
import app.meucarrinho.application.sync.port.LoggedOp;
import app.meucarrinho.application.sync.port.SyncOp;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.Result;
import app.meucarrinho.testfixtures.common.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryChangeLog implements ChangeLog, Transactional {
    private record ListLog(List<LoggedOp> ops, long lastSeq) {
        ListLog {
            ops = List.copyOf(ops);
        }
    }

    private final Map<ListId, ListLog> logs = new HashMap<>();

    @Override
    public Result<LoggedOp, ChangeLogError> append(ListId list, SyncOp op, ActorRef actor, Instant appliedAt) {
        ListLog log = logs.getOrDefault(list, new ListLog(List.of(), 0));
        for (LoggedOp existing : log.ops()) {
            if (existing.op().clientOpId().equals(op.clientOpId())) {
                return Result.err(new ChangeLogError.DuplicateOp(existing));
            }
        }
        LoggedOp logged = new LoggedOp(list, log.lastSeq() + 1, op, actor, appliedAt);
        List<LoggedOp> ops = new ArrayList<>(log.ops());
        ops.add(logged);
        logs.put(list, new ListLog(ops, logged.seq()));
        return Result.ok(logged);
    }

    @Override
    public Result<List<LoggedOp>, ChangeLogError> since(ListId list, long afterSeq) {
        ListLog log = logs.getOrDefault(list, new ListLog(List.of(), 0));
        long oldestAvailable = log.ops().isEmpty() ? log.lastSeq() + 1 : log.ops().getFirst().seq();
        if (afterSeq + 1 < oldestAvailable) {
            return Result.err(new ChangeLogError.ChangesExpired(oldestAvailable));
        }
        return Result.ok(log.ops().stream().filter(op -> op.seq() > afterSeq).toList());
    }

    @Override
    public long latestSeq(ListId list) {
        return logs.getOrDefault(list, new ListLog(List.of(), 0)).lastSeq();
    }

    @Override
    public void purgeAppliedBefore(Instant cutoff) {
        logs.replaceAll((list, log) -> new ListLog(
                log.ops().stream().filter(op -> !op.appliedAt().isBefore(cutoff)).toList(), log.lastSeq()));
    }

    @Override
    public Object checkpoint() {
        return Map.copyOf(logs);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void rollbackTo(Object checkpoint) {
        logs.clear();
        logs.putAll((Map<ListId, ListLog>) checkpoint);
    }
}
