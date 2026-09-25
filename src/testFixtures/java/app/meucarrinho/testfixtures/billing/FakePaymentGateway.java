package app.meucarrinho.testfixtures.billing;

import app.meucarrinho.application.billing.port.CancelMode;
import app.meucarrinho.application.billing.port.CheckoutSession;
import app.meucarrinho.application.billing.port.PaymentError;
import app.meucarrinho.application.billing.port.PaymentGateway;
import app.meucarrinho.application.billing.port.PortalSession;
import app.meucarrinho.application.billing.port.ReturnUrls;
import app.meucarrinho.domain.entitlement.PlanId;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.Result;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

public final class FakePaymentGateway implements PaymentGateway {
    private static final PaymentError NO_BILLING = new PaymentError.ProviderUnavailable("no billing adapter in v1");
    private final List<String> calls = new ArrayList<>();

    @Override
    public Result<CheckoutSession, PaymentError> startCheckout(AccountId account, PlanId plan, ReturnUrls urls) {
        calls.add("startCheckout " + account + " " + plan.value());
        return Result.err(NO_BILLING);
    }

    @Override
    public Result<PortalSession, PaymentError> openBillingPortal(AccountId account) {
        calls.add("openBillingPortal " + account);
        return Result.err(NO_BILLING);
    }

    @Override
    public Result<@Nullable Void, PaymentError> cancel(AccountId account, CancelMode mode) {
        calls.add("cancel " + account + " " + mode);
        return Result.err(NO_BILLING);
    }

    public List<String> calls() {
        return List.copyOf(calls);
    }
}
