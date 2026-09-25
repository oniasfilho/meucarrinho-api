package app.meucarrinho.testfixtures.telemetry;

import app.meucarrinho.application.telemetry.port.BusinessMetrics;
import app.meucarrinho.application.telemetry.port.ListActivity;
import app.meucarrinho.domain.shared.Money;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RecordingBusinessMetrics implements BusinessMetrics {
    public sealed interface Recorded {
        record PurchaseFinished(Money total, Optional<Money> budgetDelta, int items, int collaborators)
                implements Recorded {}

        record Activity(ListActivity kind) implements Recorded {}

        record SyncCompleted(int ops, Duration offlineFor, int conflicts) implements Recorded {}
    }

    private final List<Recorded> recorded = new ArrayList<>();

    @Override
    public void purchaseFinished(Money total, Optional<Money> budgetDelta, int items, int collaborators) {
        recorded.add(new Recorded.PurchaseFinished(total, budgetDelta, items, collaborators));
    }

    @Override
    public void listActivity(ListActivity kind) {
        recorded.add(new Recorded.Activity(kind));
    }

    @Override
    public void syncCompleted(int ops, Duration offlineFor, int conflicts) {
        recorded.add(new Recorded.SyncCompleted(ops, offlineFor, conflicts));
    }

    public List<Recorded> recorded() {
        return List.copyOf(recorded);
    }
}
