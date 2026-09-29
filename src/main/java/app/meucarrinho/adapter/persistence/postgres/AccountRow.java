package app.meucarrinho.adapter.persistence.postgres;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

@Table("accounts")
record AccountRow(
        @Id UUID id,
        String identityProvider,
        String identitySubject,
        String displayName,
        String email,
        String sortOrder,
        boolean collaborationAlerts,
        boolean haptics,
        boolean analyticsOptOut,
        String currency,
        Instant createdAt,
        Instant deletedAt,
        @Version Long version) {}
