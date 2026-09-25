package app.meucarrinho.testfixtures.common;

import app.meucarrinho.application.common.port.AuditEntry;
import app.meucarrinho.application.common.port.AuditError;
import app.meucarrinho.application.common.port.AuditTrail;
import app.meucarrinho.domain.shared.Result;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

public final class RecordingAuditTrail implements AuditTrail, Transactional {
    private final List<AuditEntry> entries = new ArrayList<>();

    @Override
    public Result<@Nullable Void, AuditError> record(AuditEntry entry) {
        entries.add(entry);
        return Result.ok();
    }

    public List<AuditEntry> entries() {
        return List.copyOf(entries);
    }

    @Override
    public Object checkpoint() {
        return List.copyOf(entries);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void rollbackTo(Object checkpoint) {
        entries.clear();
        entries.addAll((List<AuditEntry>) checkpoint);
    }
}
