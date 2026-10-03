package app.meucarrinho.application.accounts;

import app.meucarrinho.application.accounts.port.AccountRepository;
import app.meucarrinho.domain.account.Account;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ExternalRef;
import app.meucarrinho.domain.shared.Result;
import java.util.Optional;
import java.util.function.Supplier;

public final class AccountLookupService implements GetAccount {
    private final AccountRepository accounts;

    public AccountLookupService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public Result<Account, AccountError> byId(AccountId id) {
        return live(accounts.findById(id), () -> new AccountError.AccountNotFound(id));
    }

    @Override
    public Result<Account, AccountError> byIdentity(ExternalRef identity) {
        return live(accounts.findByIdentity(identity), () -> new AccountError.UnknownIdentity(identity));
    }

    private static Result<Account, AccountError> live(Optional<Account> found, Supplier<AccountError> missing) {
        if (found.isEmpty()) {
            return Result.err(missing.get());
        }
        Account account = found.get();
        return account.isDeleted() ? Result.err(new AccountError.AccountDeleted(account.id())) : Result.ok(account);
    }
}
