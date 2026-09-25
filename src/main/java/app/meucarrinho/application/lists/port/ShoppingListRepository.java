package app.meucarrinho.application.lists.port;

import app.meucarrinho.application.common.port.PersistenceException;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ListId;
import java.util.List;
import java.util.Optional;

public interface ShoppingListRepository {
    Optional<ShoppingList> findById(ListId id);

    ShoppingList save(ShoppingList list);

    List<ShoppingList> findActiveForMember(AccountId member);
}
