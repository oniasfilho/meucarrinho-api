package app.meucarrinho.domain.list;

import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.DisplayName;
import java.time.Instant;

public record Member(ActorRef actor, DisplayName displayName, Instant joinedAt) {
    public boolean isGuest() {
        return actor instanceof ActorRef.GuestActor;
    }
}
