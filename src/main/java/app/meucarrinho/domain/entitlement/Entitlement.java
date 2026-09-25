package app.meucarrinho.domain.entitlement;

import app.meucarrinho.domain.event.DomainEvent;
import app.meucarrinho.domain.event.Recorded;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.ExternalRef;
import java.time.Instant;

public record Entitlement(
        AccountId accountId,
        PlanId plan,
        EntitlementStatus status,
        Instant validUntil,
        ExternalRef source) {
    public static Recorded<Entitlement> activate(AccountId account, PlanId plan, Instant validUntil,
            ExternalRef source, Instant now) {
        Entitlement entitlement = new Entitlement(account, plan, EntitlementStatus.ACTIVE, validUntil, source);
        return Recorded.of(entitlement, changed(entitlement, now));
    }

    public Recorded<Entitlement> lapse(Instant now) {
        Entitlement lapsed = new Entitlement(accountId, plan, EntitlementStatus.LAPSED, validUntil, source);
        return status == EntitlementStatus.LAPSED ? Recorded.of(this) : Recorded.of(lapsed, changed(lapsed, now));
    }

    public boolean isActive(Instant now) {
        return status == EntitlementStatus.ACTIVE && now.isBefore(validUntil);
    }

    private static DomainEvent changed(Entitlement e, Instant now) {
        return new DomainEvent.EntitlementChanged(e.accountId(), e.plan().value(), e.status() == EntitlementStatus.ACTIVE,
                ActorRef.account(e.accountId()), now);
    }
}
