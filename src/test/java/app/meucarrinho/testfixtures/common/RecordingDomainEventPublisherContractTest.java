package app.meucarrinho.testfixtures.common;

import app.meucarrinho.application.common.port.DomainEventPublisher;
import app.meucarrinho.domain.event.DomainEvent;
import app.meucarrinho.testfixtures.common.DomainEventPublisherContract;
import app.meucarrinho.testfixtures.common.RecordingDomainEventPublisher;
import java.util.List;

class RecordingDomainEventPublisherContractTest implements DomainEventPublisherContract {
    private final RecordingDomainEventPublisher publisher = new RecordingDomainEventPublisher();

    @Override
    public DomainEventPublisher domainEventPublisher() {
        publisher.clear();
        return publisher;
    }

    @Override
    public List<DomainEvent> observedEvents() {
        return publisher.published();
    }
}
