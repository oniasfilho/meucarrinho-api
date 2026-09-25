package app.meucarrinho.application.lists;

import app.meucarrinho.domain.list.ListError;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.shared.Result;

public interface CreateShoppingList {
    Result<ShoppingList, ListError> create(CreateListCommand command);
}
