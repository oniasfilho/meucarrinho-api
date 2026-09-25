package app.meucarrinho.domain.account;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.domain.event.DomainEvent;
import app.meucarrinho.domain.shared.CurrencyCode;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.ExternalRef;
import app.meucarrinho.testfixtures.TestIds;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AccountTest {
    private static final Instant NOW = Instant.parse("2026-09-25T15:00:00Z");

    private Account account() {
        return Account.create(TestIds.accountId(), new ExternalRef("auth0", "google-oauth2|1"),
                new DisplayName("Marina"), Optional.empty(), NOW);
    }

    @Test
    void new_accounts_get_the_default_preferences() {
        UserPreferences prefs = account().preferences();

        assertThat(prefs.sortOrder()).isEqualTo(SortOrder.ADDED);
        assertThat(prefs.collaborationAlerts()).isTrue();
        assertThat(prefs.haptics()).isTrue();
        assertThat(prefs.analyticsOptOut()).as("analytics is opt-in (LGPD)").isTrue();
        assertThat(prefs.currency()).isEqualTo(CurrencyCode.BRL);
    }

    @Test
    void a_preferences_change_touches_only_what_it_names() {
        Account updated = account().withPreferences(PreferencesChange.none().withSortOrder(SortOrder.AZ).withHaptics(false));

        assertThat(updated.preferences()).isEqualTo(new UserPreferences(SortOrder.AZ, true, false, true, CurrencyCode.BRL));
    }

    @Test
    void deleting_is_soft_and_happens_once() {
        var deleted = account().delete(NOW);

        assertThat(deleted.aggregate().isDeleted()).isTrue();
        assertThat(deleted.events()).singleElement().isInstanceOf(DomainEvent.AccountDeleted.class);
        assertThat(deleted.aggregate().delete(NOW).events()).isEmpty();
    }
}
