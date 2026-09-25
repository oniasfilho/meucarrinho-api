package app.meucarrinho.application.billing.port;

import app.meucarrinho.domain.entitlement.PlanId;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ExternalRef;
import java.time.Instant;

public sealed interface BillingEvent {
    record SubscriptionActivated(AccountId account, PlanId plan, Instant validUntil, ExternalRef ref)
            implements BillingEvent {}

    record SubscriptionLapsed(AccountId account, ExternalRef ref) implements BillingEvent {}
}
