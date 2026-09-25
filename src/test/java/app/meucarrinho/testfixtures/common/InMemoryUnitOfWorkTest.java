package app.meucarrinho.testfixtures.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.testfixtures.TestIds;
import app.meucarrinho.testfixtures.lists.InMemoryShoppingListRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InMemoryUnitOfWorkTest {
    @Test
    void undoes_every_enlisted_store_when_the_work_throws() {
        var lists = new InMemoryShoppingListRepository();
        var events = new RecordingDomainEventPublisher();
        var unitOfWork = new InMemoryUnitOfWork().enlist(lists, events);
        ShoppingList list = ShoppingList.create(TestIds.listId(), TestIds.accountId(), new ListName("Feira"),
                Optional.empty(), Optional.empty(), Instant.parse("2026-09-25T12:00:00Z"));

        assertThatThrownBy(() -> unitOfWork.execute(() -> {
            events.publish(list.pullEvents());
            lists.save(list);
            throw new IllegalStateException("boom");
        })).hasMessage("boom");

        assertThat(lists.findById(list.id())).isEmpty();
        assertThat(events.published()).isEmpty();
        assertThat(unitOfWork.rollbacks()).isEqualTo(1);
    }
}
