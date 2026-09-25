package app.meucarrinho.testfixtures.lists;

import app.meucarrinho.application.lists.port.ShoppingListRepository;

class InMemoryShoppingListRepositoryTest extends ShoppingListRepositoryContract {
    @Override
    protected ShoppingListRepository createRepository() {
        return new InMemoryShoppingListRepository();
    }
}
