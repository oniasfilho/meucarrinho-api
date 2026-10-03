package app.meucarrinho.application.accounts;

import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ExternalRef;

public sealed interface AccountError {
    record AccountNotFound(AccountId accountId) implements AccountError {}

    /** No account was ever provisioned for this identity-provider subject. */
    record UnknownIdentity(ExternalRef identity) implements AccountError {}

    record AccountDeleted(AccountId accountId) implements AccountError {}
}
