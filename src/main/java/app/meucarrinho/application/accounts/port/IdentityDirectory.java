package app.meucarrinho.application.accounts.port;

import app.meucarrinho.domain.shared.ExternalRef;
import app.meucarrinho.domain.shared.Result;
import org.jspecify.annotations.Nullable;

public interface IdentityDirectory {
    Result<IdentityProfile, IdentityError> profile(ExternalRef subject);

    Result<@Nullable Void, IdentityError> disable(ExternalRef subject);

    Result<@Nullable Void, IdentityError> delete(ExternalRef subject);
}
