package app.meucarrinho.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.application.accounts.AccountError;
import app.meucarrinho.application.accounts.GetAccount;
import app.meucarrinho.domain.account.Account;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.ExternalRef;
import app.meucarrinho.domain.shared.Result;
import app.meucarrinho.testfixtures.TestIds;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** The caller is the verified token's subject, as an Auth0 identity, and that identity's live account. */
class TokenActorTest {
    private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");
    private final AccountId marinaId = TestIds.accountId();
    private final ExternalRef marina = new ExternalRef("auth0", "marina");

    private final GetAccount accounts = new GetAccount() {
        @Override
        public Result<Account, AccountError> byId(AccountId id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Result<Account, AccountError> byIdentity(ExternalRef identity) {
            return identity.equals(marina)
                    ? Result.ok(Account.create(marinaId, marina, new DisplayName("Marina"), Optional.empty(), NOW))
                    : Result.err(new AccountError.UnknownIdentity(identity));
        }
    };
    private final TokenActor actor = new TokenActor(accounts);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void callWithTokenFor(String subject) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject(subject).issuedAt(NOW)
                .expiresAt(NOW.plusSeconds(3600)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    void a_token_for_a_signed_up_user_is_that_users_account() {
        callWithTokenFor("marina");

        assertThat(actor.identity()).contains(marina);
        assertThat(actor.account()).contains(marinaId);
    }

    @Test
    void a_token_for_someone_without_an_account_has_an_identity_but_no_account() {
        callWithTokenFor("rosa");

        assertThat(actor.identity()).contains(new ExternalRef("auth0", "rosa"));
        assertThat(actor.account()).isEmpty();
    }

    @Test
    void no_token_is_no_one() {
        assertThat(actor.identity()).isEmpty();

        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", "anonymous",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThat(actor.identity()).isEmpty();
        assertThat(actor.account()).isEmpty();
    }
}
