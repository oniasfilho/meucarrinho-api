package app.meucarrinho.application.sharing.port;

import app.meucarrinho.domain.shared.Result;
import org.jspecify.annotations.Nullable;

public interface ListChangeBroadcaster {
    Result<@Nullable Void, RealtimeError> broadcast(ListChange change);
}
