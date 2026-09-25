package app.meucarrinho.application.accounts;

import app.meucarrinho.domain.account.Account;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.EmailAddress;
import app.meucarrinho.domain.shared.ExternalRef;
import app.meucarrinho.domain.shared.Result;
import java.util.Optional;

public interface UpsertAccount {
    Result<Account, AccountError> upsert(ExternalRef identity, DisplayName displayName, Optional<EmailAddress> email);
}
