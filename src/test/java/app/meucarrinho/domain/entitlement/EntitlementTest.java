package app.meucarrinho.domain.entitlement;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.domain.shared.ExternalRef;
import app.meucarrinho.testfixtures.TestIds;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EntitlementTest {
    private static final Instant NOW = Instant.parse("2026-09-25T15:00:00Z");

    @Test
    void is_active_until_it_lapses_or_runs_out() {
        var entitlement = Entitlement.activate(TestIds.accountId(), new PlanId("plus"), NOW.plus(Duration.ofDays(30)),
                new ExternalRef("stripe", "sub_1"), NOW).aggregate();

        assertThat(entitlement.isActive(NOW)).isTrue();
        assertThat(entitlement.isActive(NOW.plus(Duration.ofDays(30)))).isFalse();
        assertThat(entitlement.lapse(NOW).aggregate().isActive(NOW)).isFalse();
    }
}
