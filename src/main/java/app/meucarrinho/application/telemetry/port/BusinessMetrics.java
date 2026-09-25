package app.meucarrinho.application.telemetry.port;

import app.meucarrinho.domain.shared.Money;
import java.time.Duration;
import java.util.Optional;

public interface BusinessMetrics {
    void purchaseFinished(Money total, Optional<Money> budgetDelta, int items, int collaborators);

    void listActivity(ListActivity kind);

    void syncCompleted(int ops, Duration offlineFor, int conflicts);
}
