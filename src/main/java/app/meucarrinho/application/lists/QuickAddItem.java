package app.meucarrinho.application.lists;

import app.meucarrinho.domain.list.ListError;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ItemId;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.Result;
import java.util.Optional;

public interface QuickAddItem {
    Result<ShoppingList, ListError> quickAdd(ListId list, ActorRef actor, Optional<ItemId> itemId, String text);
}
