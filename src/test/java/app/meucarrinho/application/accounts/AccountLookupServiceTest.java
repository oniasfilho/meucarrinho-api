package app.meucarrinho.application.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.domain.account.Account;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.ExternalRef;
import app.meucarrinho.testfixtures.InMemoryCore;
import app.meucarrinho.testfixtures.TestIds;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AccountLookupServiceTest {
    private final InMemoryCore core = new InMemoryCore();
    private final ExternalRef subject = new ExternalRef("auth0", "google-oauth2|marina");

    private Account signedIn() {
        return core.accounts.upsert(subject, new DisplayName("Marina"), Optional.empty()).orElseThrow();
    }

    @Test
    void finds_an_account_by_id_and_by_the_identity_behind_a_token() {
        Account account = signedIn();

        assertThat(core.accountLookup.byId(account.id()).orElseThrow()).isEqualTo(account);
        assertThat(core.accountLookup.byIdentity(subject).orElseThrow()).isEqualTo(account);
    }

    @Test
    void tells_an_unknown_id_from_an_identity_that_never_signed_in() {
        AccountId unknown = TestIds.accountId();
        ExternalRef stranger = new ExternalRef("auth0", "apple|nobody");

        assertThat(core.accountLookup.byId(unknown).errorOrThrow()).isEqualTo(new AccountError.AccountNotFound(unknown));
        assertThat(core.accountLookup.byIdentity(stranger).errorOrThrow())
                .isEqualTo(new AccountError.UnknownIdentity(stranger));
    }

    @Test
    void a_deleted_account_is_refused_by_both_lookups() {
        Account account = signedIn();
        core.accountRepository.save(account.delete(core.clock.now()).aggregate());

        assertThat(core.accountLookup.byId(account.id()).errorOrThrow())
                .isEqualTo(new AccountError.AccountDeleted(account.id()));
        assertThat(core.accountLookup.byIdentity(subject).errorOrThrow())
                .isEqualTo(new AccountError.AccountDeleted(account.id()));
    }
}
