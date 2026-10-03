package app.meucarrinho.bootstrap;

import app.meucarrinho.api.rest.CurrentActor;
import app.meucarrinho.application.accounts.GetAccount;
import app.meucarrinho.domain.account.Account;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ExternalRef;
import app.meucarrinho.domain.shared.Result;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * The caller from the bearer token Spring Security verified (issuer, signature, expiry, audience). The token subject
 * is the identity; {@link GetAccount#byIdentity} turns it into an account, so a deleted account cannot act.
 */
final class TokenActor implements CurrentActor {
    /** Locally mock-oauth2 stands in for Auth0, so its subjects live in the same namespace. */
    static final String PROVIDER = "auth0";

    private final GetAccount accounts;

    TokenActor(GetAccount accounts) {
        this.accounts = accounts;
    }

    @Override
    public Optional<ExternalRef> identity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token && token.getToken().getSubject() != null) {
            return Optional.of(new ExternalRef(PROVIDER, token.getToken().getSubject()));
        }
        return Optional.empty();
    }

    @Override
    public Optional<AccountId> account() {
        return identity().flatMap(identity -> switch (accounts.byIdentity(identity)) {
            case Result.Ok<Account, ?> ok -> Optional.of(ok.value().id());
            case Result.Err<Account, ?> err -> Optional.empty();
        });
    }
}
