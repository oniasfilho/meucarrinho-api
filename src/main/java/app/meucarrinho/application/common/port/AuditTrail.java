package app.meucarrinho.application.common.port;

import app.meucarrinho.domain.shared.Result;
import org.jspecify.annotations.Nullable;

public interface AuditTrail {
    Result<@Nullable Void, AuditError> record(AuditEntry entry);
}
