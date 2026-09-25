package app.meucarrinho.domain.event;

import java.util.List;

public record Recorded<T>(T aggregate, List<DomainEvent> events) {
    public Recorded {
        events = List.copyOf(events);
    }

    public static <T> Recorded<T> of(T aggregate, DomainEvent... events) {
        return new Recorded<>(aggregate, List.of(events));
    }
}
