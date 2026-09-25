package app.meucarrinho.application.billing.port;

import app.meucarrinho.domain.entitlement.Entitlement;
import app.meucarrinho.domain.shared.AccountId;
import java.util.Optional;

public interface EntitlementRepository {
    Optional<Entitlement> findByAccount(AccountId account);

    Entitlement save(Entitlement entitlement);
}
