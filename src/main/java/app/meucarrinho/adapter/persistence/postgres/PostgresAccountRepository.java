package app.meucarrinho.adapter.persistence.postgres;

import app.meucarrinho.application.accounts.port.AccountRepository;
import app.meucarrinho.application.common.port.PersistenceException;
import app.meucarrinho.domain.account.Account;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ExternalRef;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

final class PostgresAccountRepository implements AccountRepository {
    private static final String COLUMNS = "id, identity_provider, identity_subject, display_name, email, "
            + "sort_order, collaboration_alerts, haptics, analytics_opt_out, currency, created_at, deleted_at, version";
    private final JdbcTemplate jdbc;
    private final AccountRowMapper mapper = new AccountRowMapper();

    PostgresAccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        return PersistenceErrors.translate(() -> jdbc
                .query("SELECT " + COLUMNS + " FROM accounts WHERE id = ?", mapper, id.value())
                .stream().findFirst().map(mapper::toDomain));
    }

    @Override
    public Optional<Account> findByIdentity(ExternalRef identity) {
        return PersistenceErrors.translate(() -> jdbc
                .query("SELECT " + COLUMNS + " FROM accounts WHERE identity_provider = ? AND identity_subject = ?",
                        mapper, identity.provider(), identity.value())
                .stream().findFirst().map(mapper::toDomain));
    }

    @Override
    @Transactional
    public Account save(Account account) {
        return PersistenceErrors.translate(() -> saveAccount(account));
    }

    private Account saveAccount(Account account) {
        long expected = account.version();
        long next = expected + 1;
        AccountRow row = mapper.fromDomain(account, next);
        if (expected == 0) {
            int inserted = jdbc.update("INSERT INTO accounts (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) "
                            + "ON CONFLICT DO NOTHING",
                        row.id(), row.identityProvider(), row.identitySubject(), row.displayName(), row.email(),
                        row.sortOrder(), row.collaborationAlerts(), row.haptics(), row.analyticsOptOut(),
                        row.currency(), Timestamp.from(row.createdAt()), timestamp(row.deletedAt()), next);
            if (inserted == 0) {
                long existingVersion = currentVersion(account.id());
                if (existingVersion == 0) {
                    existingVersion = identityVersion(account.identity());
                }
                throw concurrent(account.id().toString(), expected, existingVersion);
            }
        } else {
            int updated = jdbc.update("UPDATE accounts SET identity_provider=?, identity_subject=?, display_name=?, "
                                + "email=?, sort_order=?, collaboration_alerts=?, haptics=?, analytics_opt_out=?, "
                                + "currency=?, created_at=?, deleted_at=?, version=version+1 WHERE id=? AND version=?",
                        row.identityProvider(), row.identitySubject(), row.displayName(), row.email(), row.sortOrder(),
                        row.collaborationAlerts(), row.haptics(), row.analyticsOptOut(), row.currency(),
                        Timestamp.from(row.createdAt()),
                        timestamp(row.deletedAt()), row.id(), expected);
            if (updated == 0) {
                throw concurrent(account.id().toString(), expected, currentVersion(account.id()));
            }
        }
        return mapper.toDomain(mapper.fromDomain(account, next));
    }

    private long currentVersion(AccountId id) {
        return jdbc.query("SELECT version FROM accounts WHERE id = ?", rs -> rs.next() ? rs.getLong(1) : 0L, id.value());
    }

    private long identityVersion(ExternalRef identity) {
        Long version = jdbc.query("SELECT version FROM accounts WHERE identity_provider = ? AND identity_subject = ?",
                rs -> rs.next() ? rs.getLong(1) : 0L, identity.provider(), identity.value());
        return version;
    }

    private static Timestamp timestamp(java.time.Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static PersistenceException.ConcurrentModification concurrent(String id, long expected, long current) {
        return new PersistenceException.ConcurrentModification(id, expected, current);
    }
}
