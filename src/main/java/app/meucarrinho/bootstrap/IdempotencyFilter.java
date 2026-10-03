package app.meucarrinho.bootstrap;

import app.meucarrinho.api.rest.ApiProblem;
import app.meucarrinho.api.rest.CurrentActor;
import app.meucarrinho.application.common.port.IdempotencyClaim;
import app.meucarrinho.application.common.port.IdempotencyError;
import app.meucarrinho.application.common.port.IdempotencyKey;
import app.meucarrinho.application.common.port.IdempotencyStore;
import app.meucarrinho.application.common.port.RequestFingerprint;
import app.meucarrinho.application.common.port.StoredResponse;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Makes a mutating {@code /v1} call that carries {@code Idempotency-Key} safe to retry (spec §7, ADR 0010). The first
 * call runs and, if it succeeds, its response is stored; a retry of the same request gets that response back, marked
 * {@code Idempotent-Replayed: true}, without reaching a use case. It lives in bootstrap because the REST layer may not
 * depend on output ports (spec §11). Problems go through the MVC exception resolvers, so they render exactly like
 * every other error.
 */
final class IdempotencyFilter extends OncePerRequestFilter {
    static final String KEY_HEADER = "Idempotency-Key";
    static final String REPLAYED_HEADER = "Idempotent-Replayed";

    private static final Logger LOG = LoggerFactory.getLogger(IdempotencyFilter.class);
    private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final List<String> REPLAYED_HEADERS =
            List.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.ETAG, HttpHeaders.LOCATION);

    private final IdempotencyStore store;
    private final CurrentActor actor;
    private final HandlerExceptionResolver problems;

    IdempotencyFilter(IdempotencyStore store, CurrentActor actor, HandlerExceptionResolver problems) {
        this.store = store;
        this.actor = actor;
        this.problems = problems;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !MUTATING.contains(request.getMethod()) || !request.getRequestURI().startsWith("/v1/")
                || request.getHeader(KEY_HEADER) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<AccountId> account = actor.account();
        if (account.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        String value = request.getHeader(KEY_HEADER);
        if (!IdempotencyKey.isWellFormed(value)) {
            reject(request, response, ApiProblem.malformed(
                    "Idempotency-Key must be 1 to 128 characters of A-Z a-z 0-9 . _ : -"));
            return;
        }
        var key = new IdempotencyKey(account.get(), value);
        var cached = new CachedBodyRequest(request);
        RequestFingerprint fingerprint = fingerprint(cached);

        switch (store.claim(key, fingerprint)) {
            case Result.Err<IdempotencyClaim, IdempotencyError> err -> unavailable(request, response, err.error());
            case Result.Ok<IdempotencyClaim, IdempotencyError>(IdempotencyClaim claim) -> {
                switch (claim) {
                    case IdempotencyClaim.Acquired acquired -> run(cached, response, chain, key, fingerprint);
                    case IdempotencyClaim.Completed completed -> replay(response, completed.response());
                    case IdempotencyClaim.InProgress inProgress -> {
                        response.setHeader(HttpHeaders.RETRY_AFTER, "1");
                        reject(request, response, new ApiProblem(HttpStatus.CONFLICT,
                                "IDEMPOTENCY_REQUEST_IN_PROGRESS",
                                "A request with this Idempotency-Key is still running."));
                    }
                    case IdempotencyClaim.KeyReused reused -> reject(request, response, new ApiProblem(
                            HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                            "This Idempotency-Key was used for a different request."));
                }
            }
        }
    }

    private void run(HttpServletRequest request, HttpServletResponse response, FilterChain chain, IdempotencyKey key,
            RequestFingerprint fingerprint) throws ServletException, IOException {
        var recorded = new ContentCachingResponseWrapper(response);
        boolean stored = false;
        try {
            chain.doFilter(request, recorded);
            if (isSuccessful(recorded.getStatus())) {
                stored = store.complete(key, fingerprint, snapshot(recorded)).isOk();
                if (!stored) {
                    LOG.warn("Could not store the response for an Idempotency-Key; a retry will run again");
                }
            }
        } finally {
            if (!stored) {
                store.release(key, fingerprint);
            }
            recorded.copyBodyToResponse();
        }
    }

    /**
     * Only successes are stored. A use case commits all or nothing, so a 4xx or 5xx changed nothing: running the
     * retry again is safe, and lets a client fix its request and try again with the same key.
     */
    private static boolean isSuccessful(int status) {
        return status >= 200 && status < 300;
    }

    private static StoredResponse snapshot(ContentCachingResponseWrapper response) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : REPLAYED_HEADERS) {
            String header = name.equals(HttpHeaders.CONTENT_TYPE) ? response.getContentType() : response.getHeader(name);
            if (header != null) {
                headers.put(name, header);
            }
        }
        return new StoredResponse(response.getStatus(), headers, response.getContentAsByteArray());
    }

    private static void replay(HttpServletResponse response, StoredResponse stored) throws IOException {
        response.setStatus(stored.status());
        stored.headers().forEach(response::setHeader);
        response.setHeader(REPLAYED_HEADER, "true");
        byte[] body = stored.body();
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }

    private void unavailable(HttpServletRequest request, HttpServletResponse response, IdempotencyError error)
            throws ServletException {
        LOG.warn("Idempotency store unavailable: {}", error);
        response.setHeader(HttpHeaders.RETRY_AFTER, "1");
        reject(request, response, new ApiProblem(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE",
                "A service this call depends on is unavailable. Retry with the same Idempotency-Key."));
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, ApiProblem problem)
            throws ServletException {
        if (problems.resolveException(request, response, null, problem) == null) {
            throw new ServletException(problem);
        }
    }

    /** Method, path and query, If-Match and the body bytes: what makes two requests the same request. */
    private static RequestFingerprint fingerprint(CachedBodyRequest request) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            String query = request.getQueryString();
            String ifMatch = request.getHeader(HttpHeaders.IF_MATCH);
            String head = request.getMethod() + "\n" + request.getRequestURI() + (query == null ? "" : "?" + query)
                    + "\n" + (ifMatch == null ? "" : ifMatch) + "\n";
            sha256.update(head.getBytes(StandardCharsets.UTF_8));
            sha256.update(request.body);
            return new RequestFingerprint(HexFormat.of().formatHex(sha256.digest()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every JDK has SHA-256", e);
        }
    }

    /** Reads the body once, up front, so it can be hashed and still be read by the controller. */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        @Override
        public ServletInputStream getInputStream() {
            var bytes = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return bytes.read();
                }

                @Override
                public int read(byte[] buffer, int offset, int length) {
                    return bytes.read(buffer, offset, length);
                }

                @Override
                public boolean isFinished() {
                    return bytes.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(@Nullable ReadListener listener) {
                    throw new UnsupportedOperationException("The body is already buffered");
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            String encoding = getCharacterEncoding();
            Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }
}
