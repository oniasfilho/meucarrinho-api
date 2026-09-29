package app.meucarrinho.adapter.persistence.postgres;

import java.util.UUID;
import org.springframework.data.relational.core.mapping.Table;

@Table("receipt_participants")
record ReceiptParticipantRow(
        UUID receiptId, int participantIndex, String actorKind, UUID actorId) {}
