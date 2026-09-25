package app.meucarrinho.application.lists.port;

import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.StoreName;
import java.util.List;

public interface ListQueries {
    List<ListCard> activeCards(AccountId member);

    List<StoreName> recentStores(AccountId member, int limit);
}
