package app.meucarrinho.application.lists;

import app.meucarrinho.domain.list.ItemDraft;
import app.meucarrinho.domain.list.ListError;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ItemId;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.Result;
import java.util.Optional;

public interface AddItem {
    Result<ShoppingList, ListError> add(ListId list, ActorRef actor, Optional<ItemId> itemId, ItemDraft draft);
}
