package app.meucarrinho.adapter.persistence.postgres;

import app.meucarrinho.application.lists.port.ListCard;
import app.meucarrinho.application.lists.port.ListQueries;
import app.meucarrinho.application.lists.port.ShoppingListRepository;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.StoreName;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

final class PostgresListQueries implements ListQueries {
    private final ShoppingListRepository lists;
    private final JdbcTemplate jdbc;

    PostgresListQueries(ShoppingListRepository lists, JdbcTemplate jdbc) {
        this.lists = lists;
        this.jdbc = jdbc;
    }

    @Override
    public List<ListCard> activeCards(AccountId member) {
        return PersistenceErrors.translate(() -> lists.findActiveForMember(member).stream()
                .map(list -> new ListCard(list.id(), list.name(), list.store(), list.status(), list.totals(),
                        list.members().size() + 1, list.updatedAt()))
                .toList());
    }

    @Override
    public List<StoreName> recentStores(AccountId member, int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative");
        }
        if (limit == 0) {
            return List.of();
        }
        return PersistenceErrors.translate(() -> {
            List<String> stores = jdbc.query(
                    "SELECT l.store_name FROM shopping_lists l "
                            + "LEFT JOIN shopping_list_members m ON m.list_id = l.id "
                            + "WHERE l.owner_id = ? OR (m.actor_kind = 'ACCOUNT' AND m.actor_id = ?) "
                            + "ORDER BY l.updated_at DESC, l.id DESC",
                    (rs, n) -> rs.getString(1), member.value(), member.value());
            LinkedHashMap<String, StoreName> unique = new LinkedHashMap<>();
            stores.stream().filter(java.util.Objects::nonNull).map(StoreName::new)
                    .forEach(store -> unique.putIfAbsent(store.normalized(), store));
            return unique.values().stream().limit(limit).toList();
        });
    }
}
