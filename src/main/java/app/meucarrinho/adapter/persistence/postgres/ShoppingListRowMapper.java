package app.meucarrinho.adapter.persistence.postgres;

import app.meucarrinho.domain.list.ItemSnapshot;
import app.meucarrinho.domain.list.ListStatus;
import app.meucarrinho.domain.list.Member;
import app.meucarrinho.domain.list.ShoppingListSnapshot;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.Budget;
import app.meucarrinho.domain.shared.ItemId;
import app.meucarrinho.domain.shared.ItemName;
import app.meucarrinho.domain.shared.ItemNote;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.Money;
import app.meucarrinho.domain.shared.PhotoRef;
import app.meucarrinho.domain.shared.Quantity;
import app.meucarrinho.domain.shared.ReceiptId;
import app.meucarrinho.domain.shared.StoreName;
import app.meucarrinho.domain.shared.Unit;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

final class ShoppingListRowMapper implements RowMapper<ShoppingListRow> {
    @Override
    public ShoppingListRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ShoppingListRow(
                rs.getObject("id", UUID.class),
                rs.getObject("owner_id", UUID.class),
                rs.getString("name"),
                rs.getString("store_name"),
                (Long) rs.getObject("budget_minor_units"),
                rs.getString("currency").strip(),
                rs.getString("status"),
                rs.getString("status_before_delete"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                instant(rs, "completed_at"),
                instant(rs, "deleted_at"),
                rs.getObject("receipt_id", UUID.class),
                rs.getLong("version"));
    }

    ShoppingListSnapshot toSnapshot(
            ShoppingListRow row, List<ShoppingListMemberRow> members, List<ShoppingListItemRow> items) {
        return new ShoppingListSnapshot(
                new ListId(row.id()),
                new AccountId(row.ownerId()),
                new ListName(row.name()),
                Optional.ofNullable(row.storeName()).map(StoreName::new),
                Optional.ofNullable(row.budgetMinorUnits()).map(Budget::brl),
                ListStatus.valueOf(row.status()),
                Optional.ofNullable(row.statusBeforeDelete()).map(ListStatus::valueOf),
                members.stream()
                        .map(m -> new Member(ActorRefMapper.from(m.actorKind(), m.actorId()),
                                new app.meucarrinho.domain.shared.DisplayName(m.displayName()), m.joinedAt()))
                        .toList(),
                items.stream().map(this::toSnapshot).toList(),
                row.version(),
                row.createdAt(),
                row.updatedAt(),
                Optional.ofNullable(row.completedAt()),
                Optional.ofNullable(row.deletedAt()),
                Optional.ofNullable(row.receiptId()).map(ReceiptId::new));
    }

    ShoppingListRow fromSnapshot(ShoppingListSnapshot s, long version) {
        return new ShoppingListRow(
                s.id().value(), s.ownerId().value(), s.name().value(),
                s.store().map(StoreName::value).orElse(null),
                s.budget().map(b -> b.amount().minorUnits()).orElse(null), "BRL",
                s.status().name(), s.statusBeforeDelete().map(Enum::name).orElse(null),
                s.createdAt(), s.updatedAt(), s.completedAt().orElse(null), s.deletedAt().orElse(null),
                s.receiptId().map(ReceiptId::value).orElse(null), version);
    }

    ShoppingListMemberRow memberRow(Member member, int index, UUID listId) {
        return new ShoppingListMemberRow(
                listId, index, ActorRefMapper.kind(member.actor()), ActorRefMapper.id(member.actor()),
                member.displayName().value(), member.joinedAt());
    }

    ShoppingListItemRow itemRow(ItemSnapshot item, int index, UUID listId) {
        return new ShoppingListItemRow(
                item.id().value(), listId, index, item.name().value(), item.quantity().amount(),
                item.quantity().unit().name(), item.unitPrice().map(Money::minorUnits).orElse(null), "BRL",
                item.note().map(ItemNote::value).orElse(null), item.photoRef().map(PhotoRef::key).orElse(null),
                item.picked(), item.pickedBy().map(ActorRefMapper::kind).orElse(null),
                item.pickedBy().map(ActorRefMapper::id).orElse(null), ActorRefMapper.kind(item.lastEditedBy()),
                ActorRefMapper.id(item.lastEditedBy()), item.position(), item.removedAt().orElse(null));
    }

    private ItemSnapshot toSnapshot(ShoppingListItemRow item) {
        return new ItemSnapshot(
                new ItemId(item.itemId()), new ItemName(item.name()),
                new Quantity(item.quantity(), Unit.valueOf(item.unit())),
                Optional.ofNullable(item.unitPriceMinorUnits()).map(Money::brl),
                Optional.ofNullable(item.note()).map(ItemNote::new),
                Optional.ofNullable(item.photoRef()).map(PhotoRef::new),
                item.picked(), pickedBy(item.pickedByKind(), item.pickedById()),
                ActorRefMapper.from(item.lastEditedByKind(), item.lastEditedById()), item.position(),
                Optional.ofNullable(item.removedAt()));
    }

    private Optional<app.meucarrinho.domain.shared.ActorRef> pickedBy(String kind, UUID id) {
        return kind == null ? Optional.empty() : Optional.of(ActorRefMapper.from(kind, id));
    }

    private static java.time.Instant instant(ResultSet rs, String name) throws SQLException {
        return rs.getTimestamp(name) == null ? null : rs.getTimestamp(name).toInstant();
    }
}
