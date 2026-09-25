package app.meucarrinho.application.billing.port;

import app.meucarrinho.domain.entitlement.PlanId;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.Result;
import org.jspecify.annotations.Nullable;

public interface PaymentGateway {
    Result<CheckoutSession, PaymentError> startCheckout(AccountId account, PlanId plan, ReturnUrls urls);

    Result<PortalSession, PaymentError> openBillingPortal(AccountId account);

    Result<@Nullable Void, PaymentError> cancel(AccountId account, CancelMode mode);
}
