package app.meucarrinho.application.common.port;

import app.meucarrinho.domain.event.DomainEvent;
import java.util.List;

public interface DomainEventPublisher {
    void publish(List<DomainEvent> events);
}
