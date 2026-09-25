package app.meucarrinho.testfixtures.telemetry;

import app.meucarrinho.application.telemetry.port.ProductAnalytics;
import app.meucarrinho.application.telemetry.port.ProductEvent;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.InstallId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RecordingProductAnalytics implements ProductAnalytics {
    private final List<ProductEvent> events = new ArrayList<>();
    private final List<Map.Entry<InstallId, AccountId>> identified = new ArrayList<>();

    @Override
    public void track(ProductEvent event) {
        events.add(event);
    }

    @Override
    public void identify(InstallId install, AccountId account) {
        identified.add(Map.entry(install, account));
    }

    public List<ProductEvent> events() {
        return List.copyOf(events);
    }

    public <E extends ProductEvent> List<E> events(Class<E> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }

    public List<Map.Entry<InstallId, AccountId>> identified() {
        return List.copyOf(identified);
    }
}
