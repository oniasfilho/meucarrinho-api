package app.meucarrinho.application.sharing.port;

import app.meucarrinho.domain.invitation.Invitation;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ShareCode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InvitationRepository {
    Optional<Invitation> findByCode(ShareCode code);

    Invitation save(Invitation invitation);

    boolean isCodeActive(ShareCode code, Instant now);

    List<Invitation> findByList(ListId listId);
}
