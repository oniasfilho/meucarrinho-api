package app.meucarrinho.testfixtures.sharing;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.application.sharing.port.GuestPass;
import app.meucarrinho.application.sharing.port.GuestPassError;
import app.meucarrinho.application.sharing.port.GuestPassStore;
import app.meucarrinho.application.sharing.port.GuestPassToken;
import app.meucarrinho.application.sharing.port.IssuedGuestPass;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.GuestId;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.testfixtures.TestIds;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class GuestPassStoreContract {
    private static final Instant T0 = Instant.parse("2026-09-25T12:00:00Z");
    private GuestPassStore store;
    private final ListId list = TestIds.listId();
    private final GuestId guest = TestIds.guestId();

    protected abstract GuestPassStore createStore();

    @BeforeEach
    void setUp() {
        store = createStore();
    }

    private IssuedGuestPass issue() {
        return store.issue(list, guest, new DisplayName("Tia Rosa"), T0).orElseThrow();
    }

    @Test
    void issues_a_pass_bound_to_one_list_for_30_days() {
        IssuedGuestPass issued = issue();

        assertThat(issued.pass()).isEqualTo(new GuestPass(list, guest, new DisplayName("Tia Rosa"), T0.plus(Duration.ofDays(30))));
        assertThat(issued.token().value()).as("256 bits, base64url").hasSizeGreaterThanOrEqualTo(43);
        assertThat(store.verify(issued.token(), T0).orElseThrow().listId()).isEqualTo(list);
    }

    @Test
    void issues_a_different_token_every_time() {
        assertThat(issue().token()).isNotEqualTo(issue().token());
    }

    @Test
    void extends_the_pass_on_each_use() {
        IssuedGuestPass issued = issue();
        Instant later = T0.plus(Duration.ofDays(20));

        assertThat(store.verify(issued.token(), later).orElseThrow().expiresAt()).isEqualTo(later.plus(Duration.ofDays(30)));
        assertThat(store.verify(issued.token(), T0.plus(Duration.ofDays(40))).isOk()).isTrue();
    }

    @Test
    void refuses_unknown_expired_and_revoked_passes() {
        IssuedGuestPass issued = issue();

        assertThat(store.verify(new GuestPassToken("x".repeat(43)), T0).errorOrThrow())
                .isEqualTo(new GuestPassError.Unknown());
        assertThat(store.verify(issued.token(), T0.plus(Duration.ofDays(30))).errorOrThrow())
                .isEqualTo(new GuestPassError.Expired());

        IssuedGuestPass other = store.issue(list, guest, new DisplayName("Tia Rosa"), T0).orElseThrow();
        store.revoke(list, guest).orElseThrow();
        store.revoke(list, guest).orElseThrow();
        assertThat(store.verify(other.token(), T0).errorOrThrow()).isEqualTo(new GuestPassError.Revoked());
    }
}
