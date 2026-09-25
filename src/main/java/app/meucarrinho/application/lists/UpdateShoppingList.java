package app.meucarrinho.application.lists;

import app.meucarrinho.domain.list.ListDetailsChange;
import app.meucarrinho.domain.list.ListError;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.Result;

public interface UpdateShoppingList {
    Result<ShoppingList, ListError> update(ListId id, ActorRef actor, ListDetailsChange change);
}
