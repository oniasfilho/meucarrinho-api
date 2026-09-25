package app.meucarrinho.testfixtures.sharing;

import app.meucarrinho.application.sharing.port.InvitationRepository;
import app.meucarrinho.domain.invitation.Invitation;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ShareCode;
import app.meucarrinho.testfixtures.common.Transactional;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryInvitationRepository implements InvitationRepository, Transactional {
    private final Map<ShareCode, Invitation> invitations = new HashMap<>();

    @Override
    public Optional<Invitation> findByCode(ShareCode code) {
        return Optional.ofNullable(invitations.get(code));
    }

    @Override
    public Invitation save(Invitation invitation) {
        invitations.put(invitation.code(), invitation);
        return invitation;
    }

    @Override
    public boolean isCodeActive(ShareCode code, Instant now) {
        return findByCode(code).map(invitation -> invitation.isActive(now)).orElse(false);
    }

    @Override
    public List<Invitation> findByList(ListId listId) {
        return invitations.values().stream()
                .filter(invitation -> invitation.listId().equals(listId))
                .sorted(Comparator.comparing(Invitation::createdAt).reversed())
                .toList();
    }

    @Override
    public Object checkpoint() {
        return Map.copyOf(invitations);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void rollbackTo(Object checkpoint) {
        invitations.clear();
        invitations.putAll((Map<ShareCode, Invitation>) checkpoint);
    }
}
