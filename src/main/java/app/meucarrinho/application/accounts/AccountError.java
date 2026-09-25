package app.meucarrinho.application.accounts;

import app.meucarrinho.domain.shared.AccountId;

public sealed interface AccountError {
    record AccountNotFound(AccountId accountId) implements AccountError {}

    record AccountDeleted(AccountId accountId) implements AccountError {}
}
