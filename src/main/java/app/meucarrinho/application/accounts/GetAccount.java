package app.meucarrinho.application.accounts;

import app.meucarrinho.domain.account.Account;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ExternalRef;
import app.meucarrinho.domain.shared.Result;

public interface GetAccount {
    Result<Account, AccountError> byId(AccountId id);

    /** Turns a verified token subject into its account; the auth layer calls this on every request. */
    Result<Account, AccountError> byIdentity(ExternalRef identity);
}
