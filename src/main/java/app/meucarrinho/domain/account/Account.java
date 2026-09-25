package app.meucarrinho.domain.account;

import app.meucarrinho.domain.event.DomainEvent;
import app.meucarrinho.domain.event.Recorded;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.EmailAddress;
import app.meucarrinho.domain.shared.ExternalRef;
import java.time.Instant;
import java.util.Optional;

public record Account(
        AccountId id,
        ExternalRef identity,
        DisplayName displayName,
        Optional<EmailAddress> email,
        UserPreferences preferences,
        Instant createdAt,
        Optional<Instant> deletedAt,
        long version) {
    public static Account create(AccountId id, ExternalRef identity, DisplayName displayName,
            Optional<EmailAddress> email, Instant now) {
        return new Account(id, identity, displayName, email, UserPreferences.defaults(), now, Optional.empty(), 0);
    }

    public Account withProfile(DisplayName newName, Optional<EmailAddress> newEmail) {
        return new Account(id, identity, newName, newEmail, preferences, createdAt, deletedAt, version);
    }

    public Account withPreferences(PreferencesChange change) {
        return new Account(id, identity, displayName, email, preferences.apply(change), createdAt, deletedAt, version);
    }

    public Account withVersion(long newVersion) {
        return new Account(id, identity, displayName, email, preferences, createdAt, deletedAt, newVersion);
    }

    public Recorded<Account> delete(Instant now) {
        if (isDeleted()) {
            return Recorded.of(this);
        }
        Account deleted = new Account(id, identity, displayName, email, preferences, createdAt, Optional.of(now), version);
        return Recorded.of(deleted, new DomainEvent.AccountDeleted(id, ActorRef.account(id), now));
    }

    public boolean isDeleted() {
        return deletedAt.isPresent();
    }
}
