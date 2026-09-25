package app.meucarrinho.testfixtures.sharing;

import app.meucarrinho.application.sharing.port.PresenceTracker;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ListId;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class InMemoryPresenceTracker implements PresenceTracker {
    private final Map<ListId, Map<ActorRef, Instant>> lastSeen = new HashMap<>();

    @Override
    public void heartbeat(ListId list, ActorRef member, Instant now) {
        lastSeen.computeIfAbsent(list, id -> new HashMap<>()).put(member, now);
    }

    @Override
    public Set<ActorRef> online(ListId list, Instant now) {
        return lastSeen.getOrDefault(list, Map.of()).entrySet().stream()
                .filter(seen -> !seen.getValue().plus(TTL).isBefore(now))
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }
}
