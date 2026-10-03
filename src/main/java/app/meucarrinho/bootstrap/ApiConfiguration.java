package app.meucarrinho.bootstrap;

import app.meucarrinho.application.common.port.Clock;
import java.time.InstantSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** What the REST layer needs from outside the core: what time it is. Who is calling is in {@link SecurityConfiguration}. */
@Configuration(proxyBeanMethods = false)
class ApiConfiguration {
    /** The core's clock for the REST layer, which may not depend on output ports (spec §11). */
    @Bean
    InstantSource instantSource(Clock clock) {
        return clock::now;
    }
}
