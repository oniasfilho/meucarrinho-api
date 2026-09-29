package app.meucarrinho.adapter.persistence.postgres;

import app.meucarrinho.domain.account.Account;
import app.meucarrinho.domain.account.SortOrder;
import app.meucarrinho.domain.account.UserPreferences;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.CurrencyCode;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.EmailAddress;
import app.meucarrinho.domain.shared.ExternalRef;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

final class AccountRowMapper implements RowMapper<AccountRow> {
    @Override
    public AccountRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new AccountRow(
                rs.getObject("id", UUID.class),
                rs.getString("identity_provider"),
                rs.getString("identity_subject"),
                rs.getString("display_name"),
                rs.getString("email"),
                rs.getString("sort_order"),
                rs.getBoolean("collaboration_alerts"),
                rs.getBoolean("haptics"),
                rs.getBoolean("analytics_opt_out"),
                rs.getString("currency").strip(),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("deleted_at") == null ? null : rs.getTimestamp("deleted_at").toInstant(),
                rs.getLong("version"));
    }

    Account toDomain(AccountRow row) {
        return new Account(
                new AccountId(row.id()),
                new ExternalRef(row.identityProvider(), row.identitySubject()),
                new DisplayName(row.displayName()),
                Optional.ofNullable(row.email()).map(EmailAddress::new),
                new UserPreferences(
                        SortOrder.valueOf(row.sortOrder()),
                        row.collaborationAlerts(),
                        row.haptics(),
                        row.analyticsOptOut(),
                        CurrencyCode.valueOf(row.currency())),
                row.createdAt(),
                Optional.ofNullable(row.deletedAt()),
                row.version());
    }

    AccountRow fromDomain(Account account, long version) {
        return new AccountRow(
                account.id().value(),
                account.identity().provider(),
                account.identity().value(),
                account.displayName().value(),
                account.email().map(EmailAddress::value).orElse(null),
                account.preferences().sortOrder().name(),
                account.preferences().collaborationAlerts(),
                account.preferences().haptics(),
                account.preferences().analyticsOptOut(),
                account.preferences().currency().name(),
                account.createdAt(),
                account.deletedAt().orElse(null),
                version);
    }
}
