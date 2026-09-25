package app.meucarrinho.testfixtures.common;

import app.meucarrinho.application.common.port.UnitOfWork;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class InMemoryUnitOfWork implements UnitOfWork {
    private final List<Transactional> participants = new ArrayList<>();
    private int depth;
    private int commits;
    private int rollbacks;

    public InMemoryUnitOfWork enlist(Transactional... stores) {
        participants.addAll(List.of(stores));
        return this;
    }

    @Override
    public <T> T execute(Supplier<T> work) {
        if (depth > 0) {
            return work.get();
        }
        Map<Transactional, Object> checkpoints = new IdentityHashMap<>();
        participants.forEach(store -> checkpoints.put(store, store.checkpoint()));
        depth++;
        try {
            T result = work.get();
            commits++;
            return result;
        } catch (RuntimeException e) {
            checkpoints.forEach(Transactional::rollbackTo);
            rollbacks++;
            throw e;
        } finally {
            depth--;
        }
    }

    public int commits() {
        return commits;
    }

    public int rollbacks() {
        return rollbacks;
    }

    public boolean inTransaction() {
        return depth > 0;
    }
}
