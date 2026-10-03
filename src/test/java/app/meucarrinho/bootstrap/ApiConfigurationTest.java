package app.meucarrinho.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ApiConfigurationTest {
    private final ApiConfiguration configuration = new ApiConfiguration();

    @Test
    void until_step_5_no_request_has_an_actor() {
        assertThat(configuration.currentActor().account()).isEmpty();
    }

    @Test
    void the_rest_layer_reads_the_cores_clock() {
        Instant now = Instant.parse("2026-10-03T12:00:00Z");

        assertThat(configuration.instantSource(() -> now).instant()).isEqualTo(now);
    }
}
