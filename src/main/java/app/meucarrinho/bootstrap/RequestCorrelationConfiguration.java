package app.meucarrinho.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers {@link RequestCorrelationFilter} so every response carries {@code X-Request-Id} (spec §13). */
@Configuration(proxyBeanMethods = false)
class RequestCorrelationConfiguration {
    @Bean
    RequestCorrelationFilter requestCorrelationFilter() {
        return new RequestCorrelationFilter();
    }
}
