package app.meucarrinho.testfixtures.accounts;

import app.meucarrinho.application.accounts.port.AccountRepository;

class InMemoryAccountRepositoryTest extends AccountRepositoryContract {
    @Override
    protected AccountRepository createRepository() {
        return new InMemoryAccountRepository();
    }
}
