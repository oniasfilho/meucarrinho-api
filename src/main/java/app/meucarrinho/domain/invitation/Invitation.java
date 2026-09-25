package app.meucarrinho.domain.invitation;

import app.meucarrinho.domain.event.DomainEvent;
import app.meucarrinho.domain.event.Recorded;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.Result;
import app.meucarrinho.domain.shared.ShareCode;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public record Invitation(
        ShareCode code,
        ListId listId,
        AccountId createdBy,
        Instant createdAt,
        Instant expiresAt,
        Optional<Integer> maxUses,
        int uses,
        Optional<Instant> revokedAt) {
    public static final Duration DEFAULT_TTL = Duration.ofDays(7);

    public Invitation {
        maxUses.ifPresent(max -> {
            if (max < 1) {
                throw new IllegalArgumentException("maxUses must be at least 1");
            }
        });
        if (uses < 0) {
            throw new IllegalArgumentException("uses cannot be negative");
        }
    }

    public static Recorded<Invitation> create(ShareCode code, ListId listId, AccountId createdBy,
            Optional<Integer> maxUses, Instant now) {
        Invitation invitation = new Invitation(code, listId, createdBy, now, now.plus(DEFAULT_TTL), maxUses, 0,
                Optional.empty());
        return Recorded.of(invitation,
                new DomainEvent.InvitationCreated(listId, code, invitation.expiresAt(), ActorRef.account(createdBy), now));
    }

    public Result<Invitation, InvitationError> check(Instant now) {
        if (revokedAt.isPresent()) {
            return Result.err(new InvitationError.Revoked());
        }
        if (!now.isBefore(expiresAt)) {
            return Result.err(new InvitationError.Expired());
        }
        if (maxUses.isPresent() && uses >= maxUses.get()) {
            return Result.err(new InvitationError.UsedUp(maxUses.get()));
        }
        return Result.ok(this);
    }

    public boolean isActive(Instant now) {
        return check(now).isOk();
    }

    public Result<Invitation, InvitationError> accept(Instant now) {
        return check(now).map(ok ->
                new Invitation(code, listId, createdBy, createdAt, expiresAt, maxUses, uses + 1, revokedAt));
    }

    public Invitation revoke(Instant now) {
        return revokedAt.isPresent() ? this
                : new Invitation(code, listId, createdBy, createdAt, expiresAt, maxUses, uses, Optional.of(now));
    }
}
