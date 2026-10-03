package app.meucarrinho.application.lists;

import app.meucarrinho.application.lists.port.ListQueries;
import app.meucarrinho.domain.shared.AccountId;
import java.util.List;

public final class ActiveListsService implements ListActiveLists {
    private final ListQueries queries;

    public ActiveListsService(ListQueries queries) {
        this.queries = queries;
    }

    @Override
    public List<ActiveListCard> activeLists(AccountId member) {
        return queries.activeCards(member).stream()
                .map(card -> new ActiveListCard(card.id(), card.name(), card.store(), card.status(), card.totals(),
                        card.people(), card.updatedAt()))
                .toList();
    }
}
