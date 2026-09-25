package app.meucarrinho.testfixtures.billing;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.application.billing.port.EntitlementRepository;
import app.meucarrinho.domain.entitlement.Entitlement;
import app.meucarrinho.domain.entitlement.EntitlementStatus;
import app.meucarrinho.domain.entitlement.PlanId;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ExternalRef;
import app.meucarrinho.testfixtures.TestIds;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class EntitlementRepositoryContract {
    private static final Instant T0 = Instant.parse("2026-09-25T12:00:00Z");
    private EntitlementRepository repository;

    protected abstract EntitlementRepository createRepository();

    @BeforeEach
    void setUp() {
        repository = createRepository();
    }

    @Test
    void keeps_one_entitlement_per_account() {
        AccountId account = TestIds.accountId();
        Entitlement active = Entitlement.activate(account, new PlanId("plus"), T0.plusSeconds(3600),
                new ExternalRef("stripe", "sub_1"), T0).aggregate();

        repository.save(active);
        repository.save(active.lapse(T0).aggregate());

        assertThat(repository.findByAccount(account).orElseThrow().status()).isEqualTo(EntitlementStatus.LAPSED);
        assertThat(repository.findByAccount(TestIds.accountId())).isEmpty();
    }
}
