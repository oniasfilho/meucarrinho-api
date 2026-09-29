package app.meucarrinho.adapter.persistence.postgres;

import app.meucarrinho.application.common.port.UnitOfWork;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

final class SpringTransactionUnitOfWork implements UnitOfWork {
    private final TransactionTemplate transactions;

    SpringTransactionUnitOfWork(TransactionTemplate transactions) {
        this.transactions = transactions;
    }

    @Override
    public <T> T execute(Supplier<T> work) {
        return transactions.execute(status -> work.get());
    }
}
