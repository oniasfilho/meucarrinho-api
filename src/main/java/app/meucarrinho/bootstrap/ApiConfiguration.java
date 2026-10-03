package app.meucarrinho.bootstrap;

import app.meucarrinho.api.rest.CurrentActor;
import app.meucarrinho.application.common.port.Clock;
import java.time.InstantSource;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** What the REST layer needs from outside the core: who is calling, and what time it is. */
@Configuration(proxyBeanMethods = false)
class ApiConfiguration {
    /**
     * No authentication exists before session 2, step 5, so no request has an actor and every endpoint answers 401.
     * Step 5 replaces this with an actor built from the verified token and {@code GetAccount.byIdentity}. There is
     * deliberately no development header or other back door.
     */
    @Bean
    CurrentActor currentActor() {
        return Optional::empty;
    }

    /** The core's clock for the REST layer, which may not depend on output ports (spec §11). */
    @Bean
    InstantSource instantSource(Clock clock) {
        return clock::now;
    }
}
