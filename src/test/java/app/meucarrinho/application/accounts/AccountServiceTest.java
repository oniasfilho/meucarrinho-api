package app.meucarrinho.application.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.domain.account.Account;
import app.meucarrinho.domain.account.PreferencesChange;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.EmailAddress;
import app.meucarrinho.domain.shared.ExternalRef;
import app.meucarrinho.testfixtures.InMemoryCore;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AccountServiceTest {
    private final InMemoryCore core = new InMemoryCore();
    private final ExternalRef subject = new ExternalRef("auth0", "apple|123");

    @Test
    void one_account_per_identity_however_often_the_app_signs_in() {
        Account first = core.accounts.upsert(subject, new DisplayName("Onias"), Optional.empty()).orElseThrow();
        Account again = core.accounts.upsert(subject, new DisplayName("Onias"), Optional.empty()).orElseThrow();
        Account renamed = core.accounts.upsert(subject, new DisplayName("Onias Filho"),
                Optional.of(new EmailAddress("onias@example.com"))).orElseThrow();

        assertThat(again).isEqualTo(first);
        assertThat(renamed.id()).isEqualTo(first.id());
        assertThat(renamed.displayName()).isEqualTo(new DisplayName("Onias Filho"));
        assertThat(renamed.version()).isEqualTo(2);
    }

    @Test
    void preferences_change_one_field_at_a_time() {
        Account account = core.accounts.upsert(subject, new DisplayName("Onias"), Optional.empty()).orElseThrow();

        Account updated = core.accounts.update(account.id(), PreferencesChange.none().withAnalyticsOptOut(false))
                .orElseThrow();

        assertThat(updated.preferences().analyticsOptOut()).isFalse();
        assertThat(updated.preferences().haptics()).isTrue();
    }

    @Test
    void a_deleted_account_is_not_revived_by_signing_in() {
        Account account = core.accounts.upsert(subject, new DisplayName("Onias"), Optional.empty()).orElseThrow();
        core.accountRepository.save(account.delete(core.clock.now()).aggregate());

        assertThat(core.accounts.upsert(subject, new DisplayName("Onias"), Optional.empty()).errorOrThrow())
                .isEqualTo(new AccountError.AccountDeleted(account.id()));
    }
}
