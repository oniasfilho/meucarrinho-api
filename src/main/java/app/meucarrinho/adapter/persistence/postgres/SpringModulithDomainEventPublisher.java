package app.meucarrinho.adapter.persistence.postgres;

import app.meucarrinho.application.common.port.DomainEventPublisher;
import app.meucarrinho.domain.event.DomainEvent;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;

final class SpringModulithDomainEventPublisher implements DomainEventPublisher {
    private final ApplicationEventPublisher events;

    SpringModulithDomainEventPublisher(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Override
    public void publish(List<DomainEvent> domainEvents) {
        domainEvents.forEach(events::publishEvent);
    }
}
