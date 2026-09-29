package app.meucarrinho.testfixtures.lists;

import app.meucarrinho.application.lists.port.ListQueries;
import app.meucarrinho.application.lists.port.ShoppingListRepository;

class InMemoryListQueriesContractTest extends ListQueriesContract {
    private InMemoryShoppingListRepository repository;

    @Override
    protected Fixture createFixture() {
        repository = new InMemoryShoppingListRepository();
        return new Fixture(repository, new InMemoryListQueries(repository));
    }
}
