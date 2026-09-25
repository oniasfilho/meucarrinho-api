package app.meucarrinho.application.accounts;

import app.meucarrinho.domain.account.Account;
import app.meucarrinho.domain.account.PreferencesChange;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.Result;

public interface UpdatePreferences {
    Result<Account, AccountError> update(AccountId account, PreferencesChange change);
}
