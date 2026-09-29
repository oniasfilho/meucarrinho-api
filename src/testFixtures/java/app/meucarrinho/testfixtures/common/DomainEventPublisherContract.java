package app.meucarrinho.testfixtures.common;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.application.common.port.DomainEventPublisher;
import app.meucarrinho.domain.event.DomainEvent;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.testfixtures.TestIds;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

public interface DomainEventPublisherContract {
    DomainEventPublisher domainEventPublisher();

    List<DomainEvent> observedEvents();

    @Test
    default void forwards_all_events_in_input_order() {
        DomainEvent first = new DomainEvent.ItemAdded(
                TestIds.listId(), TestIds.itemId(), ActorRef.account(TestIds.accountId()),
                Instant.parse("2026-09-29T12:00:00Z"));
        DomainEvent second = new DomainEvent.ItemPicked(
                TestIds.listId(), TestIds.itemId(), ActorRef.account(TestIds.accountId()),
                Instant.parse("2026-09-29T12:01:00Z"));
        List<DomainEvent> expected = List.of(first, second);

        domainEventPublisher().publish(expected);

        assertThat(observedEvents()).containsExactlyElementsOf(expected);
    }
}
