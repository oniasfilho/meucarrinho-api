package app.meucarrinho.application.sharing.port;

import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ListId;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public interface PresenceTracker {
    Duration TTL = Duration.ofSeconds(45);

    void heartbeat(ListId list, ActorRef member, Instant now);

    Set<ActorRef> online(ListId list, Instant now);
}
