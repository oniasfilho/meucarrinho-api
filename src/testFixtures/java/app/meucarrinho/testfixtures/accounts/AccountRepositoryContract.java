package app.meucarrinho.testfixtures.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.meucarrinho.application.accounts.port.AccountRepository;
import app.meucarrinho.application.common.port.PersistenceException;
import app.meucarrinho.domain.account.Account;
import app.meucarrinho.domain.account.PreferencesChange;
import app.meucarrinho.domain.account.SortOrder;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.EmailAddress;
import app.meucarrinho.domain.shared.ExternalRef;
import app.meucarrinho.testfixtures.TestIds;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class AccountRepositoryContract {
    private static final Instant T0 = Instant.parse("2026-09-25T12:00:00Z");
    private AccountRepository repository;

    protected abstract AccountRepository createRepository();

    @BeforeEach
    void setUp() {
        repository = createRepository();
    }

    private Account account(String subject) {
        return Account.create(TestIds.accountId(), new ExternalRef("auth0", subject), new DisplayName("Marina"),
                Optional.of(new EmailAddress("marina@example.com")), T0);
    }

    @Test
    void saves_and_finds_by_id_and_by_identity() {
        Account saved = repository.save(account("google-oauth2|1"));

        assertThat(saved.version()).isEqualTo(1);
        assertThat(repository.findById(saved.id())).contains(saved);
        assertThat(repository.findByIdentity(new ExternalRef("auth0", "google-oauth2|1"))).contains(saved);
        assertThat(repository.findByIdentity(new ExternalRef("auth0", "apple|1"))).isEmpty();
    }

    @Test
    void updates_with_optimistic_locking() {
        Account saved = repository.save(account("google-oauth2|1"));
        Account updated = repository.save(saved.withPreferences(PreferencesChange.none().withSortOrder(SortOrder.AZ)));

        assertThat(updated.version()).isEqualTo(2);
        assertThat(repository.findById(saved.id()).orElseThrow().preferences().sortOrder()).isEqualTo(SortOrder.AZ);
        assertThatThrownBy(() -> repository.save(saved)).isInstanceOf(PersistenceException.ConcurrentModification.class);
    }

    @Test
    void keeps_one_account_per_identity() {
        repository.save(account("google-oauth2|1"));

        assertThatThrownBy(() -> repository.save(account("google-oauth2|1")))
                .isInstanceOf(PersistenceException.ConcurrentModification.class);
    }
}
