package app.meucarrinho.application.billing.port;

import app.meucarrinho.domain.shared.AccountId;

public sealed interface PaymentError {
    record ProviderUnavailable(String reason) implements PaymentError {}

    record CustomerNotFound(AccountId account) implements PaymentError {}

    record Rejected(String userMessageKey) implements PaymentError {}
}
