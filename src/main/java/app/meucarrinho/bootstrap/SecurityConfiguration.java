package app.meucarrinho.bootstrap;

import app.meucarrinho.api.rest.ApiProblem;
import app.meucarrinho.api.rest.CurrentActor;
import app.meucarrinho.application.accounts.GetAccount;
import jakarta.servlet.ServletException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Bearer tokens only (spec §6, §12): stateless, no cookies, no CSRF, no login page. A valid token sets the caller; a
 * call without one reaches the controller anonymously and gets the same {@code 401 UNAUTHENTICATED} problem, so the
 * public endpoints of later sessions need no change here. A bad or expired token is refused before any controller.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {
    @Bean
    SecurityFilterChain api(HttpSecurity http,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver problems) throws Exception {
        AuthenticationEntryPoint badToken = (request, response, failure) -> {
            if (problems.resolveException(request, response, null, new ApiProblem(HttpStatus.UNAUTHORIZED,
                    "UNAUTHENTICATED", "The access token is invalid or expired.")) == null) {
                throw new ServletException(failure);
            }
        };
        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .oauth2ResourceServer(tokens -> tokens.jwt(Customizer.withDefaults()).authenticationEntryPoint(badToken))
                .build();
    }

    @Bean
    CurrentActor currentActor(GetAccount accounts) {
        return new TokenActor(accounts);
    }
}
