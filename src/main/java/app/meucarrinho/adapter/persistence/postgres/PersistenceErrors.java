package app.meucarrinho.adapter.persistence.postgres;

import app.meucarrinho.application.common.port.PersistenceException;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;

final class PersistenceErrors {
    private PersistenceErrors() {}

    static <T> T translate(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (DataAccessException e) {
            throw new PersistenceException.StoreUnavailable("PostgreSQL persistence operation failed");
        }
    }

    static void translate(Runnable operation) {
        translate(() -> {
            operation.run();
            return null;
        });
    }
}
