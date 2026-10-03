package app.meucarrinho.api.rest.me;

import app.meucarrinho.domain.account.Account;
import app.meucarrinho.domain.account.SortOrder;
import app.meucarrinho.domain.account.UserPreferences;
import app.meucarrinho.domain.shared.CurrencyCode;
import app.meucarrinho.domain.shared.EmailAddress;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record MeResponse(UUID id, String displayName, @Nullable String email, Preferences preferences,
        Instant createdAt) {
    public record Preferences(SortOrder sortOrder, boolean collaborationAlerts, boolean haptics,
            boolean analyticsOptOut, CurrencyCode currency) {}

    static MeResponse of(Account account) {
        UserPreferences p = account.preferences();
        return new MeResponse(account.id().value(), account.displayName().value(),
                account.email().map(EmailAddress::value).orElse(null),
                new Preferences(p.sortOrder(), p.collaborationAlerts(), p.haptics(), p.analyticsOptOut(),
                        p.currency()),
                account.createdAt());
    }
}
