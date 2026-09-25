package app.meucarrinho.domain.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.domain.event.DomainEvent;
import app.meucarrinho.domain.shared.ShareCode;
import app.meucarrinho.testfixtures.TestIds;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InvitationTest {
    private static final Instant NOW = Instant.parse("2026-09-25T15:00:00Z");

    private Invitation invitation(Optional<Integer> maxUses) {
        var created = Invitation.create(new ShareCode("4K7Q"), TestIds.listId(), TestIds.accountId(), maxUses, NOW);
        assertThat(created.events()).singleElement().isInstanceOf(DomainEvent.InvitationCreated.class);
        return created.aggregate();
    }

    @Test
    void expires_after_seven_days() {
        Invitation invitation = invitation(Optional.empty());

        assertThat(invitation.isActive(NOW.plus(Duration.ofDays(7)).minusSeconds(1))).isTrue();
        assertThat(invitation.check(NOW.plus(Duration.ofDays(7))).errorOrThrow()).isEqualTo(new InvitationError.Expired());
    }

    @Test
    void counts_uses_up_to_its_cap() {
        Invitation once = invitation(Optional.of(1)).accept(NOW).orElseThrow();

        assertThat(once.uses()).isEqualTo(1);
        assertThat(once.accept(NOW).errorOrThrow()).isEqualTo(new InvitationError.UsedUp(1));
    }

    @Test
    void can_be_revoked_by_the_owner() {
        Invitation revoked = invitation(Optional.empty()).revoke(NOW);

        assertThat(revoked.accept(NOW).errorOrThrow()).isEqualTo(new InvitationError.Revoked());
    }
}
