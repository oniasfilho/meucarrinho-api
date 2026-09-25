package app.meucarrinho.testfixtures.accounts;

import app.meucarrinho.application.accounts.port.AccountRepository;
import app.meucarrinho.application.common.port.PersistenceException;
import app.meucarrinho.domain.account.Account;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ExternalRef;
import app.meucarrinho.testfixtures.common.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryAccountRepository implements AccountRepository, Transactional {
    private final Map<AccountId, Account> accounts = new HashMap<>();

    @Override
    public Optional<Account> findById(AccountId id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public Optional<Account> findByIdentity(ExternalRef identity) {
        return accounts.values().stream().filter(a -> a.identity().equals(identity)).findFirst();
    }

    @Override
    public Account save(Account account) {
        long stored = findById(account.id()).map(Account::version).orElse(0L);
        if (stored != account.version()) {
            throw new PersistenceException.ConcurrentModification(account.id().toString(), account.version(), stored);
        }
        findByIdentity(account.identity()).filter(other -> !other.id().equals(account.id())).ifPresent(other -> {
            throw new PersistenceException.ConcurrentModification(account.id().toString(), account.version(), stored);
        });
        Account saved = account.withVersion(stored + 1);
        accounts.put(saved.id(), saved);
        return saved;
    }

    @Override
    public Object checkpoint() {
        return Map.copyOf(accounts);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void rollbackTo(Object checkpoint) {
        accounts.clear();
        accounts.putAll((Map<AccountId, Account>) checkpoint);
    }
}
