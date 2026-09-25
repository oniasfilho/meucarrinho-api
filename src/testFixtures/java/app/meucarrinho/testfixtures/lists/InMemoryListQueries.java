package app.meucarrinho.testfixtures.lists;

import app.meucarrinho.application.lists.port.ListCard;
import app.meucarrinho.application.lists.port.ListQueries;
import app.meucarrinho.domain.list.ListAccessPolicy;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.StoreName;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

public final class InMemoryListQueries implements ListQueries {
    private final InMemoryShoppingListRepository lists;

    public InMemoryListQueries(InMemoryShoppingListRepository lists) {
        this.lists = lists;
    }

    @Override
    public List<ListCard> activeCards(AccountId member) {
        return lists.findActiveForMember(member).stream()
                .map(list -> new ListCard(list.id(), list.name(), list.store(), list.status(), list.totals(),
                        list.members().size() + 1, list.updatedAt()))
                .toList();
    }

    @Override
    public List<StoreName> recentStores(AccountId member, int limit) {
        var byKey = new LinkedHashMap<String, StoreName>();
        lists.all().stream()
                .filter(list -> ListAccessPolicy.roleOf(list, ActorRef.account(member)) != ListAccessPolicy.Role.NONE)
                .sorted(Comparator.comparing(ShoppingList::updatedAt).reversed())
                .flatMap(list -> list.store().stream())
                .forEach(store -> byKey.putIfAbsent(store.normalized(), store));
        return byKey.values().stream().limit(limit).toList();
    }
}
