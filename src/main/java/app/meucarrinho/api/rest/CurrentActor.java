package app.meucarrinho.api.rest;

import app.meucarrinho.domain.shared.AccountId;
import java.util.Optional;

/**
 * Who is calling. Controllers ask this instead of reading tokens; {@code bootstrap} supplies the implementation
 * (until step 5 there is none that authenticates, so every call is anonymous and gets 401).
 */
public interface CurrentActor {
    Optional<AccountId> account();

    default AccountId require() {
        return account().orElseThrow(ApiProblem::unauthenticated);
    }
}
