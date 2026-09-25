package app.meucarrinho.testfixtures.sharing;

import app.meucarrinho.application.sharing.port.ListChange;
import app.meucarrinho.application.sharing.port.ListChangeBroadcaster;
import app.meucarrinho.application.sharing.port.RealtimeError;
import app.meucarrinho.domain.shared.Result;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

public final class RecordingListChangeBroadcaster implements ListChangeBroadcaster {
    private final List<ListChange> sent = new ArrayList<>();

    @Override
    public Result<@Nullable Void, RealtimeError> broadcast(ListChange change) {
        sent.add(change);
        return Result.ok();
    }

    public List<ListChange> sent() {
        return List.copyOf(sent);
    }
}
