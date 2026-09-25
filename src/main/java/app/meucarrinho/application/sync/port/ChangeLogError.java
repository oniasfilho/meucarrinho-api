package app.meucarrinho.application.sync.port;

public sealed interface ChangeLogError {
    record DuplicateOp(LoggedOp existing) implements ChangeLogError {}

    record ChangesExpired(long oldestAvailableSeq) implements ChangeLogError {}

    record StoreUnavailable(String reason) implements ChangeLogError {}
}
