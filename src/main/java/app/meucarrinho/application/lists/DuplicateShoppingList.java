package app.meucarrinho.application.lists;

import app.meucarrinho.domain.list.ListError;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.Result;
import java.util.Optional;

public interface DuplicateShoppingList {
    Result<ShoppingList, ListError> duplicate(ListId source, AccountId actor, Optional<ListId> newListId);
}
