package app.meucarrinho.testfixtures.billing;

import app.meucarrinho.application.billing.port.BillingEvent;
import app.meucarrinho.application.billing.port.BillingEventSource;
import app.meucarrinho.application.billing.port.RawWebhook;
import app.meucarrinho.application.billing.port.WebhookError;
import app.meucarrinho.domain.shared.Result;
import java.util.Optional;

public final class FakeBillingEventSource implements BillingEventSource {
    private Result<Optional<BillingEvent>, WebhookError> next = Result.err(new WebhookError.InvalidSignature());

    public FakeBillingEventSource willReturn(Result<Optional<BillingEvent>, WebhookError> result) {
        this.next = result;
        return this;
    }

    @Override
    public Result<Optional<BillingEvent>, WebhookError> translate(RawWebhook request) {
        return next;
    }
}
