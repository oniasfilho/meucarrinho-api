package app.meucarrinho.adapter.persistence.postgres;

import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.receipt.ReceiptLine;
import app.meucarrinho.domain.shared.Budget;
import app.meucarrinho.domain.shared.ItemName;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.Money;
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

final class ReceiptRowMapper implements RowMapper<ReceiptRow> {
    @Override
    public ReceiptRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ReceiptRow(
                rs.getObject("id", UUID.class), rs.getObject("list_id", UUID.class),
                rs.getString("list_name"), rs.getString("store_name"),
                rs.getTimestamp("completed_at").toInstant(), rs.getString("finished_by_kind"),
                rs.getObject("finished_by_id", UUID.class), rs.getLong("total_minor_units"),
                (Long) rs.getObject("budget_minor_units"), (Long) rs.getObject("budget_delta_minor_units"),
                rs.getString("currency").strip());
    }

    Receipt toDomain(ReceiptRow row, List<ReceiptParticipantRow> participants, List<ReceiptLineRow> lines) {
        List<ReceiptLine> domainLines = lines.stream()
                .map(line -> ReceiptLine.of(
                        new ItemName(line.name()), new Quantity(line.quantity(), Unit.valueOf(line.unit())),
                        Optional.ofNullable(line.unitPriceMinorUnits()).map(Money::brl)))
                .toList();
        Receipt receipt = Receipt.of(
                new ReceiptId(row.id()), new ListId(row.listId()), new ListName(row.listName()),
                Optional.ofNullable(row.storeName()).map(StoreName::new), row.completedAt(),
                ActorRefMapper.from(row.finishedByKind(), row.finishedById()),
                participants.stream().map(p -> ActorRefMapper.from(p.actorKind(), p.actorId())).toList(),
                domainLines, Optional.ofNullable(row.budgetMinorUnits()).map(Budget::brl));
        if (receipt.total().minorUnits() != row.totalMinorUnits()
                || !receipt.budgetDelta().map(Money::minorUnits).equals(Optional.ofNullable(row.budgetDeltaMinorUnits()))) {
            throw new IllegalStateException("Receipt persistence snapshot is inconsistent: " + row.id());
        }
        return receipt;
    }

    ReceiptRow fromDomain(Receipt receipt) {
        return new ReceiptRow(
                receipt.id().value(), receipt.listId().value(), receipt.name().value(),
                receipt.store().map(StoreName::value).orElse(null), receipt.completedAt(),
                ActorRefMapper.kind(receipt.finishedBy()), ActorRefMapper.id(receipt.finishedBy()),
                receipt.total().minorUnits(), receipt.budget().map(b -> b.amount().minorUnits()).orElse(null),
                receipt.budgetDelta().map(Money::minorUnits).orElse(null), "BRL");
    }

    ReceiptParticipantRow participantRow(ReceiptId id, int index, app.meucarrinho.domain.shared.ActorRef actor) {
        return new ReceiptParticipantRow(id.value(), index, ActorRefMapper.kind(actor), ActorRefMapper.id(actor));
    }

    ReceiptLineRow lineRow(ReceiptId id, int index, ReceiptLine line) {
        return new ReceiptLineRow(
                id.value(), index, line.name().value(), line.quantity().amount(), line.quantity().unit().name(),
                line.unitPrice().map(Money::minorUnits).orElse(null), line.subtotal().minorUnits(), "BRL");
    }
}
