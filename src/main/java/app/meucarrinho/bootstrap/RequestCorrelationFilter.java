package app.meucarrinho.bootstrap;

import app.meucarrinho.api.rest.RequestCorrelation;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Mints or echoes {@code X-Request-Id} and mints a {@code traceId} for every call (spec §13), before any other
 * filter sees the request, so security, idempotency and the controllers all run under one correlation.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
final class RequestCorrelationFilter extends OncePerRequestFilter {
    static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var correlation = RequestCorrelation.start(request.getHeader(REQUEST_ID_HEADER));
        response.setHeader(REQUEST_ID_HEADER, correlation.requestId());
        try {
            chain.doFilter(request, response);
        } finally {
            RequestCorrelation.clear();
        }
    }
}
