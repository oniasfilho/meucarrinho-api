package app.meucarrinho.application.capabilities.api;

import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.Result;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public interface CapabilityPolicy {
    Set<Capability> resolve(ActorRef actor, ClientContext client);

    Result<@Nullable Void, CapabilityUnavailable> require(ActorRef actor, ClientContext client, Capability capability);
}
