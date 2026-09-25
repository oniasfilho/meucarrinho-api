package app.meucarrinho.testfixtures.common;

import app.meucarrinho.application.common.port.DomainEventPublisher;
import app.meucarrinho.domain.event.DomainEvent;
import java.util.ArrayList;
import java.util.List;

public final class RecordingDomainEventPublisher implements DomainEventPublisher, Transactional {
    private final List<DomainEvent> published = new ArrayList<>();

    @Override
    public void publish(List<DomainEvent> events) {
        published.addAll(events);
    }

    public List<DomainEvent> published() {
        return List.copyOf(published);
    }

    public <E extends DomainEvent> List<E> published(Class<E> type) {
        return published.stream().filter(type::isInstance).map(type::cast).toList();
    }

    public void clear() {
        published.clear();
    }

    @Override
    public Object checkpoint() {
        return List.copyOf(published);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void rollbackTo(Object checkpoint) {
        published.clear();
        published.addAll((List<DomainEvent>) checkpoint);
    }
}
