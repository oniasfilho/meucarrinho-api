package app.meucarrinho.application.billing.port;

import app.meucarrinho.domain.shared.Result;
import java.util.Optional;

public interface BillingEventSource {
    Result<Optional<BillingEvent>, WebhookError> translate(RawWebhook request);
}
