package app.meucarrinho.testfixtures.receipts;

import app.meucarrinho.application.common.port.PersistenceException;
import app.meucarrinho.application.receipts.port.ReceiptRepository;
import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ReceiptId;
import app.meucarrinho.testfixtures.common.Transactional;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryReceiptRepository implements ReceiptRepository, Transactional {
    private final Map<ReceiptId, Receipt> receipts = new HashMap<>();

    @Override
    public Optional<Receipt> findById(ReceiptId id) {
        return Optional.ofNullable(receipts.get(id));
    }

    @Override
    public void save(Receipt receipt) {
        Receipt existing = receipts.putIfAbsent(receipt.id(), receipt);
        if (existing != null && !existing.equals(receipt)) {
            throw new PersistenceException.ConcurrentModification(receipt.id().toString(), 0, 1);
        }
    }

    @Override
    public List<Receipt> findVisibleTo(AccountId account, Instant from, Instant to) {
        return newestFirst(account)
                .filter(r -> !r.completedAt().isBefore(from) && r.completedAt().isBefore(to))
                .toList();
    }

    @Override
    public Optional<Receipt> findLatestVisibleTo(AccountId account) {
        return newestFirst(account).findFirst();
    }

    private java.util.stream.Stream<Receipt> newestFirst(AccountId account) {
        return receipts.values().stream()
                .filter(r -> r.isVisibleTo(account))
                .sorted(Comparator.comparing(Receipt::completedAt).thenComparing(r -> r.id().value()).reversed());
    }

    @Override
    public Object checkpoint() {
        return Map.copyOf(receipts);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void rollbackTo(Object checkpoint) {
        receipts.clear();
        receipts.putAll((Map<ReceiptId, Receipt>) checkpoint);
    }
}
