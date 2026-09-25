package app.meucarrinho.testfixtures.billing;

import app.meucarrinho.application.billing.port.EntitlementRepository;
import app.meucarrinho.domain.entitlement.Entitlement;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.testfixtures.common.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryEntitlementRepository implements EntitlementRepository, Transactional {
    private final Map<AccountId, Entitlement> entitlements = new HashMap<>();

    @Override
    public Optional<Entitlement> findByAccount(AccountId account) {
        return Optional.ofNullable(entitlements.get(account));
    }

    @Override
    public Entitlement save(Entitlement entitlement) {
        entitlements.put(entitlement.accountId(), entitlement);
        return entitlement;
    }

    @Override
    public Object checkpoint() {
        return Map.copyOf(entitlements);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void rollbackTo(Object checkpoint) {
        entitlements.clear();
        entitlements.putAll((Map<AccountId, Entitlement>) checkpoint);
    }
}
