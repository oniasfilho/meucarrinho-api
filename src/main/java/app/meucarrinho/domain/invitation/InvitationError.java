package app.meucarrinho.domain.invitation;

public sealed interface InvitationError {
    record Expired() implements InvitationError {}

    record Revoked() implements InvitationError {}

    record UsedUp(int maxUses) implements InvitationError {}
}
