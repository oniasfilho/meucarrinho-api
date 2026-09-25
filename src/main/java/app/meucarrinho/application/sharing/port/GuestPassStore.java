package app.meucarrinho.application.sharing.port;

import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.GuestId;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.Result;
import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public interface GuestPassStore {
    Duration VALIDITY = Duration.ofDays(30);

    Result<IssuedGuestPass, GuestPassError> issue(ListId list, GuestId guest, DisplayName displayName, Instant now);

    Result<GuestPass, GuestPassError> verify(GuestPassToken token, Instant now);

    Result<@Nullable Void, GuestPassError> revoke(ListId list, GuestId guest);
}
