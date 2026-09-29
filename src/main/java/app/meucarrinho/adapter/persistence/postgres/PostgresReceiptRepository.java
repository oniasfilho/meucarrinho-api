package app.meucarrinho.adapter.persistence.postgres;

import app.meucarrinho.application.common.port.PersistenceException;
import app.meucarrinho.application.receipts.port.ReceiptRepository;
import app.meucarrinho.domain.receipt.Receipt;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ReceiptId;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

final class PostgresReceiptRepository implements ReceiptRepository {
    private static final String ROOT_COLUMNS = "id, list_id, list_name, store_name, completed_at, finished_by_kind, "
            + "finished_by_id, total_minor_units, budget_minor_units, budget_delta_minor_units, currency";
    private final JdbcTemplate jdbc;
    private final ReceiptRowMapper mapper = new ReceiptRowMapper();

    PostgresReceiptRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Receipt> findById(ReceiptId id) {
        return PersistenceErrors.translate(() -> jdbc
                .query("SELECT " + ROOT_COLUMNS + " FROM receipts WHERE id = ?", mapper, id.value())
                .stream().findFirst().map(this::toDomain));
    }

    @Override
    @Transactional
    public void save(Receipt receipt) {
        PersistenceErrors.translate(() -> saveReceipt(receipt));
    }

    private void saveReceipt(Receipt receipt) {
        ReceiptRow row = mapper.fromDomain(receipt);
        int inserted = jdbc.update("INSERT INTO receipts (" + ROOT_COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?) "
                            + "ON CONFLICT (id) DO NOTHING",
                    row.id(), row.listId(), row.listName(), row.storeName(), Timestamp.from(row.completedAt()),
                    row.finishedByKind(), row.finishedById(), row.totalMinorUnits(), row.budgetMinorUnits(),
                    row.budgetDeltaMinorUnits(), row.currency());
        if (inserted == 0) {
            if (findById(receipt.id()).filter(receipt::equals).isPresent()) {
                return;
            }
            throw new PersistenceException.ConcurrentModification(receipt.id().toString(), 0, 1);
        }
        for (int i = 0; i < receipt.participants().size(); i++) {
            var participant = mapper.participantRow(receipt.id(), i, receipt.participants().get(i));
            jdbc.update("INSERT INTO receipt_participants (receipt_id, participant_index, actor_kind, actor_id) "
                            + "VALUES (?,?,?,?)",
                    participant.receiptId(), participant.participantIndex(), participant.actorKind(), participant.actorId());
        }
        for (int i = 0; i < receipt.lines().size(); i++) {
            ReceiptLineRow line = mapper.lineRow(receipt.id(), i, receipt.lines().get(i));
            jdbc.update("INSERT INTO receipt_lines (receipt_id, line_index, name, quantity, unit, "
                            + "unit_price_minor_units, subtotal_minor_units, currency) VALUES (?,?,?,?,?,?,?,?)",
                    line.receiptId(), line.lineIndex(), line.name(), line.quantity(), line.unit(),
                    line.unitPriceMinorUnits(), line.subtotalMinorUnits(), line.currency());
        }
    }

    @Override
    public List<Receipt> findVisibleTo(AccountId account, java.time.Instant from, java.time.Instant to) {
        return PersistenceErrors.translate(() -> findVisibleToRows(account, from, to));
    }

    private List<Receipt> findVisibleToRows(AccountId account, java.time.Instant from, java.time.Instant to) {
        List<ReceiptRow> rows = jdbc.query("SELECT r." + ROOT_COLUMNS.replace(", ", ", r.")
                        + " FROM receipts r WHERE EXISTS (SELECT 1 FROM receipt_participants p "
                        + "WHERE p.receipt_id = r.id AND p.actor_kind = 'ACCOUNT' AND p.actor_id = ?) "
                        + "AND r.completed_at >= ? "
                        + "AND r.completed_at < ? ORDER BY r.completed_at DESC, r.id DESC",
                mapper, account.value(), Timestamp.from(from), Timestamp.from(to));
        return rows.stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Receipt> findLatestVisibleTo(AccountId account) {
        return PersistenceErrors.translate(() -> findLatestVisibleToRow(account));
    }

    private Optional<Receipt> findLatestVisibleToRow(AccountId account) {
        List<ReceiptRow> rows = jdbc.query("SELECT r." + ROOT_COLUMNS.replace(", ", ", r.")
                        + " FROM receipts r WHERE EXISTS (SELECT 1 FROM receipt_participants p "
                        + "WHERE p.receipt_id = r.id AND p.actor_kind = 'ACCOUNT' AND p.actor_id = ?) "
                        + "ORDER BY r.completed_at DESC, r.id DESC LIMIT 1",
                mapper, account.value());
        return rows.stream().findFirst().map(this::toDomain);
    }

    private Receipt toDomain(ReceiptRow row) {
        List<ReceiptParticipantRow> participants = jdbc.query(
                "SELECT receipt_id, participant_index, actor_kind, actor_id FROM receipt_participants "
                        + "WHERE receipt_id = ? ORDER BY participant_index",
                (rs, n) -> new ReceiptParticipantRow(rs.getObject("receipt_id", UUID.class),
                        rs.getInt("participant_index"), rs.getString("actor_kind"),
                        rs.getObject("actor_id", UUID.class)), row.id());
        List<ReceiptLineRow> lines = jdbc.query(
                "SELECT receipt_id, line_index, name, quantity, unit, unit_price_minor_units, "
                        + "subtotal_minor_units, currency FROM receipt_lines WHERE receipt_id = ? ORDER BY line_index",
                (rs, n) -> new ReceiptLineRow(rs.getObject("receipt_id", UUID.class), rs.getInt("line_index"),
                        rs.getString("name"), rs.getBigDecimal("quantity"), rs.getString("unit"),
                        (Long) rs.getObject("unit_price_minor_units"), rs.getLong("subtotal_minor_units"),
                        rs.getString("currency").strip()), row.id());
        return mapper.toDomain(row, participants, lines);
    }
}
