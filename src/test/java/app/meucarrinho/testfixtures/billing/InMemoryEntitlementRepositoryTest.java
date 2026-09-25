package app.meucarrinho.testfixtures.billing;

import app.meucarrinho.application.billing.port.EntitlementRepository;

class InMemoryEntitlementRepositoryTest extends EntitlementRepositoryContract {
    @Override
    protected EntitlementRepository createRepository() {
        return new InMemoryEntitlementRepository();
    }
}
