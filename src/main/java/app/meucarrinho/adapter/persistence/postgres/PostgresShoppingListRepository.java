package app.meucarrinho.adapter.persistence.postgres;

import app.meucarrinho.application.common.port.PersistenceException;
import app.meucarrinho.application.lists.port.ShoppingListRepository;
import app.meucarrinho.domain.list.ShoppingList;
import app.meucarrinho.domain.list.ShoppingListSnapshot;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ListId;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

final class PostgresShoppingListRepository implements ShoppingListRepository {
    private static final String ROOT_COLUMNS = "id, owner_id, name, store_name, budget_minor_units, currency, "
            + "status, status_before_delete, created_at, updated_at, completed_at, deleted_at, receipt_id, version";
    private static final String MEMBER_COLUMNS = "list_id, member_index, actor_kind, actor_id, display_name, joined_at";
    private static final String ITEM_COLUMNS = "item_id, list_id, item_index, name, quantity, unit, "
            + "unit_price_minor_units, currency, note, photo_ref, picked, picked_by_kind, picked_by_id, "
            + "last_edited_by_kind, last_edited_by_id, position, removed_at";

    private final JdbcTemplate jdbc;
    private final ShoppingListRowMapper mapper = new ShoppingListRowMapper();

    PostgresShoppingListRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ShoppingList> findById(ListId id) {
        return PersistenceErrors.translate(() -> findByIdRows(id));
    }

    private Optional<ShoppingList> findByIdRows(ListId id) {
        List<ShoppingListRow> roots = jdbc.query(
                "SELECT " + ROOT_COLUMNS + " FROM shopping_lists WHERE id = ?", new ShoppingListRowMapper(), id.value());
        if (roots.isEmpty()) {
            return Optional.empty();
        }
        UUID listId = id.value();
        List<ShoppingListMemberRow> members = jdbc.query(
                "SELECT " + MEMBER_COLUMNS + " FROM shopping_list_members WHERE list_id = ? ORDER BY member_index",
                (rs, n) -> new ShoppingListMemberRow(rs.getObject("list_id", UUID.class), rs.getInt("member_index"),
                        rs.getString("actor_kind"), rs.getObject("actor_id", UUID.class),
                        rs.getString("display_name"), rs.getTimestamp("joined_at").toInstant()),
                listId);
        List<ShoppingListItemRow> items = jdbc.query(
                "SELECT " + ITEM_COLUMNS + " FROM shopping_list_items WHERE list_id = ? ORDER BY position",
                (rs, n) -> new ShoppingListItemRow(
                        rs.getObject("item_id", UUID.class), rs.getObject("list_id", UUID.class),
                        rs.getInt("item_index"), rs.getString("name"), rs.getBigDecimal("quantity"),
                        rs.getString("unit"), (Long) rs.getObject("unit_price_minor_units"),
                        rs.getString("currency").strip(), rs.getString("note"), rs.getString("photo_ref"),
                        rs.getBoolean("picked"), rs.getString("picked_by_kind"),
                        rs.getObject("picked_by_id", UUID.class), rs.getString("last_edited_by_kind"),
                        rs.getObject("last_edited_by_id", UUID.class), rs.getInt("position"),
                        rs.getTimestamp("removed_at") == null ? null : rs.getTimestamp("removed_at").toInstant()),
                listId);
        return Optional.of(ShoppingList.rehydrate(mapper.toSnapshot(roots.getFirst(), members, items)));
    }

    @Override
    @Transactional
    public ShoppingList save(ShoppingList list) {
        return PersistenceErrors.translate(() -> saveList(list));
    }

    private ShoppingList saveList(ShoppingList list) {
        ShoppingListSnapshot snapshot = list.snapshot();
        long expected = snapshot.version();
        long next = expected + 1;
        ShoppingListRow row = mapper.fromSnapshot(snapshot, next);
        if (expected == 0) {
            insertRoot(row);
        } else {
                int updated = jdbc.update("UPDATE shopping_lists SET owner_id=?, name=?, store_name=?, "
                                + "budget_minor_units=?, currency=?, status=?, status_before_delete=?, created_at=?, "
                                + "updated_at=?, completed_at=?, deleted_at=?, receipt_id=?, version=version+1 "
                                + "WHERE id=? AND version=?",
                        row.ownerId(), row.name(), row.storeName(), row.budgetMinorUnits(), row.currency(),
                        row.status(), row.statusBeforeDelete(), timestamp(row.createdAt()), timestamp(row.updatedAt()),
                        timestamp(row.completedAt()), timestamp(row.deletedAt()), row.receiptId(), row.id(), expected);
                if (updated == 0) {
                    throw concurrent(row.id(), expected, currentVersion(row.id()));
                }
        }
        replaceChildren(snapshot);
        return ShoppingList.rehydrate(snapshot.withVersion(next));
    }

    @Override
    public List<ShoppingList> findActiveForMember(AccountId member) {
        return PersistenceErrors.translate(() -> findActiveForMemberRows(member));
    }

    private List<ShoppingList> findActiveForMemberRows(AccountId member) {
        List<UUID> ids = jdbc.query("SELECT DISTINCT l.id FROM shopping_lists l "
                        + "LEFT JOIN shopping_list_members m ON m.list_id = l.id "
                        + "WHERE l.status = 'ACTIVE' AND (l.owner_id = ? OR "
                        + "(m.actor_kind = 'ACCOUNT' AND m.actor_id = ?)) "
                        + "ORDER BY l.updated_at DESC, l.id DESC",
                (rs, n) -> rs.getObject(1, UUID.class), member.value(), member.value());
        return ids.stream().map(id -> findById(new ListId(id)).orElseThrow()).toList();
    }

    private void insertRoot(ShoppingListRow row) {
        int inserted = jdbc.update("INSERT INTO shopping_lists (" + ROOT_COLUMNS + ") "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (id) DO NOTHING",
                row.id(), row.ownerId(), row.name(), row.storeName(), row.budgetMinorUnits(), row.currency(),
                row.status(), row.statusBeforeDelete(), timestamp(row.createdAt()), timestamp(row.updatedAt()),
                timestamp(row.completedAt()), timestamp(row.deletedAt()), row.receiptId(), row.version());
        if (inserted == 0) {
            throw concurrent(row.id(), 0, currentVersion(row.id()));
        }
    }

    private void replaceChildren(ShoppingListSnapshot snapshot) {
        UUID id = snapshot.id().value();
        jdbc.update("DELETE FROM shopping_list_members WHERE list_id = ?", id);
        jdbc.update("DELETE FROM shopping_list_items WHERE list_id = ?", id);
        List<ShoppingListMemberRow> members = java.util.stream.IntStream.range(0, snapshot.members().size())
                .mapToObj(i -> mapper.memberRow(snapshot.members().get(i), i, id)).toList();
        for (ShoppingListMemberRow member : members) {
            jdbc.update("INSERT INTO shopping_list_members (" + MEMBER_COLUMNS + ") VALUES (?,?,?,?,?,?)",
                    member.listId(), member.memberIndex(), member.actorKind(), member.actorId(), member.displayName(),
                    timestamp(member.joinedAt()));
        }
        List<ShoppingListItemRow> items = java.util.stream.IntStream.range(0, snapshot.items().size())
                .mapToObj(i -> mapper.itemRow(snapshot.items().get(i), i, id)).toList();
        for (ShoppingListItemRow item : items) {
            jdbc.update("INSERT INTO shopping_list_items (" + ITEM_COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    item.itemId(), item.listId(), item.itemIndex(), item.name(), item.quantity(), item.unit(),
                    item.unitPriceMinorUnits(), item.currency(), item.note(), item.photoRef(), item.picked(),
                    item.pickedByKind(), item.pickedById(), item.lastEditedByKind(), item.lastEditedById(),
                    item.position(), timestamp(item.removedAt()));
        }
    }

    private long currentVersion(UUID id) {
        return jdbc.query("SELECT version FROM shopping_lists WHERE id = ?", rs -> rs.next() ? rs.getLong(1) : 0L, id);
    }

    private static PersistenceException.ConcurrentModification concurrent(UUID id, long expected, long current) {
        return new PersistenceException.ConcurrentModification(id.toString(), expected, current);
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
