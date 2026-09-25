package app.meucarrinho.application.accounts.port;

import app.meucarrinho.application.common.port.PersistenceException;
import app.meucarrinho.domain.account.Account;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ExternalRef;
import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findById(AccountId id);

    Optional<Account> findByIdentity(ExternalRef identity);

    Account save(Account account);
}
