package app.meucarrinho.api.rest;

import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ExternalRef;
import java.util.Optional;
import org.springframework.http.HttpStatus;

/**
 * Who is calling. Controllers ask this instead of reading tokens; {@code bootstrap} supplies the implementation built
 * on the verified bearer token (spec §6).
 */
public interface CurrentActor {
    /** The verified identity behind the call, whether or not it has an account yet. */
    Optional<ExternalRef> identity();

    /** The caller's account; empty when anonymous or when the identity has no live account. */
    Optional<AccountId> account();

    default AccountId require() {
        return account().orElseThrow(() -> identity().isPresent()
                ? new ApiProblem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                        "This sign-in has no account yet. Call PUT /v1/me first.")
                : ApiProblem.unauthenticated());
    }

    default ExternalRef requireIdentity() {
        return identity().orElseThrow(ApiProblem::unauthenticated);
    }
}
