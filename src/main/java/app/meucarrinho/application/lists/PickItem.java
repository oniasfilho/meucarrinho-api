package app.meucarrinho.application.lists;

import app.meucarrinho.domain.list.ListError;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ItemId;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.Result;

public interface PickItem {
    Result<ShoppingList, ListError> pick(ListId list, ActorRef actor, ItemId itemId);
}
