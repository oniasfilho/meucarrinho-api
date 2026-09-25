package app.meucarrinho.testfixtures.lists;

import app.meucarrinho.application.common.port.PersistenceException;
import app.meucarrinho.application.lists.port.ShoppingListRepository;
import app.meucarrinho.domain.list.ListAccessPolicy;
import app.meucarrinho.domain.list.ListStatus;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.list.ShoppingListSnapshot;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.testfixtures.common.Transactional;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryShoppingListRepository implements ShoppingListRepository, Transactional {
    private final Map<ListId, ShoppingListSnapshot> lists = new HashMap<>();

    @Override
    public Optional<ShoppingList> findById(ListId id) {
        return Optional.ofNullable(lists.get(id)).map(ShoppingList::rehydrate);
    }

    @Override
    public ShoppingList save(ShoppingList list) {
        ShoppingListSnapshot incoming = list.snapshot();
        long stored = Optional.ofNullable(lists.get(list.id())).map(ShoppingListSnapshot::version).orElse(0L);
        if (stored != incoming.version()) {
            throw new PersistenceException.ConcurrentModification(list.id().toString(), incoming.version(), stored);
        }
        ShoppingListSnapshot saved = incoming.withVersion(stored + 1);
        lists.put(list.id(), saved);
        return ShoppingList.rehydrate(saved);
    }

    @Override
    public List<ShoppingList> findActiveForMember(AccountId member) {
        return all().stream()
                .filter(list -> list.status() == ListStatus.ACTIVE)
                .filter(list -> ListAccessPolicy.roleOf(list, ActorRef.account(member)) != ListAccessPolicy.Role.NONE)
                .sorted(Comparator.comparing(ShoppingList::updatedAt).reversed())
                .toList();
    }

    public List<ShoppingList> all() {
        return lists.values().stream().map(ShoppingList::rehydrate).toList();
    }

    @Override
    public Object checkpoint() {
        return Map.copyOf(lists);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void rollbackTo(Object checkpoint) {
        lists.clear();
        lists.putAll((Map<ListId, ShoppingListSnapshot>) checkpoint);
    }
}
