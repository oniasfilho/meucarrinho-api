package app.meucarrinho.testfixtures.sharing;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.application.sharing.port.InvitationRepository;
import app.meucarrinho.domain.invitation.Invitation;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ShareCode;
import app.meucarrinho.testfixtures.TestIds;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class InvitationRepositoryContract {
    private static final Instant T0 = Instant.parse("2026-09-25T12:00:00Z");
    private InvitationRepository repository;

    protected abstract InvitationRepository createRepository();

    @BeforeEach
    void setUp() {
        repository = createRepository();
    }

    private Invitation invitation(String code, ListId list, Instant at) {
        return Invitation.create(new ShareCode(code), list, TestIds.accountId(), Optional.empty(), at).aggregate();
    }

    @Test
    void saves_and_finds_by_code() {
        Invitation saved = repository.save(invitation("4K7Q", TestIds.listId(), T0));

        assertThat(repository.findByCode(new ShareCode("4K7Q"))).contains(saved);
        assertThat(repository.findByCode(new ShareCode("ZZZZ"))).isEmpty();
    }

    @Test
    void stores_uses_and_revocation() {
        Invitation saved = repository.save(invitation("4K7Q", TestIds.listId(), T0));
        repository.save(saved.accept(T0).orElseThrow().revoke(T0));

        Invitation loaded = repository.findByCode(saved.code()).orElseThrow();
        assertThat(loaded.uses()).isEqualTo(1);
        assertThat(loaded.revokedAt()).contains(T0);
    }

    @Test
    void knows_whether_a_code_is_active() {
        repository.save(invitation("4K7Q", TestIds.listId(), T0));

        assertThat(repository.isCodeActive(new ShareCode("4K7Q"), T0.plus(Duration.ofDays(6)))).isTrue();
        assertThat(repository.isCodeActive(new ShareCode("4K7Q"), T0.plus(Duration.ofDays(7)))).isFalse();
        assertThat(repository.isCodeActive(new ShareCode("ZZZZ"), T0)).isFalse();
    }

    @Test
    void lists_a_lists_invitations_newest_first() {
        ListId list = TestIds.listId();
        Invitation first = repository.save(invitation("AAAA", list, T0));
        Invitation second = repository.save(invitation("BBBB", list, T0.plusSeconds(60)));
        repository.save(invitation("CCCC", TestIds.listId(), T0));

        assertThat(repository.findByList(list)).containsExactly(second, first);
    }
}
