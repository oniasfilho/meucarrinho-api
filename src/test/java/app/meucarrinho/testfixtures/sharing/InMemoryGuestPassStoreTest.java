package app.meucarrinho.testfixtures.sharing;

import app.meucarrinho.application.sharing.port.GuestPassStore;

class InMemoryGuestPassStoreTest extends GuestPassStoreContract {
    @Override
    protected GuestPassStore createStore() {
        return new InMemoryGuestPassStore();
    }
}
