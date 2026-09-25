package app.meucarrinho.application.accounts;

import app.meucarrinho.application.accounts.port.AccountRepository;
import app.meucarrinho.application.common.port.Clock;
import app.meucarrinho.application.common.port.IdGenerator;
import app.meucarrinho.application.common.port.UnitOfWork;
import app.meucarrinho.domain.account.Account;
import app.meucarrinho.domain.account.PreferencesChange;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.EmailAddress;
import app.meucarrinho.domain.shared.ExternalRef;
import app.meucarrinho.domain.shared.Result;
import java.util.Optional;

public final class AccountService implements UpsertAccount, UpdatePreferences {
    private final AccountRepository accounts;
    private final UnitOfWork unitOfWork;
    private final IdGenerator ids;
    private final Clock clock;

    public AccountService(AccountRepository accounts, UnitOfWork unitOfWork, IdGenerator ids, Clock clock) {
        this.accounts = accounts;
        this.unitOfWork = unitOfWork;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    public Result<Account, AccountError> upsert(ExternalRef identity, DisplayName displayName,
            Optional<EmailAddress> email) {
        return unitOfWork.execute(() -> {
            Optional<Account> existing = accounts.findByIdentity(identity);
            if (existing.isEmpty()) {
                return Result.ok(accounts.save(
                        Account.create(ids.newAccountId(), identity, displayName, email, clock.now())));
            }
            Account account = existing.get();
            if (account.isDeleted()) {
                return Result.err(new AccountError.AccountDeleted(account.id()));
            }
            if (account.displayName().equals(displayName) && account.email().equals(email)) {
                return Result.ok(account);
            }
            return Result.ok(accounts.save(account.withProfile(displayName, email)));
        });
    }

    @Override
    public Result<Account, AccountError> update(AccountId id, PreferencesChange change) {
        return unitOfWork.execute(() -> {
            Optional<Account> found = accounts.findById(id);
            if (found.isEmpty()) {
                return Result.err(new AccountError.AccountNotFound(id));
            }
            if (found.get().isDeleted()) {
                return Result.err(new AccountError.AccountDeleted(id));
            }
            return Result.ok(accounts.save(found.get().withPreferences(change)));
        });
    }
}
