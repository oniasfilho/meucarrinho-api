package app.meucarrinho.api.rest;

import java.security.SecureRandom;
import java.util.HexFormat;
import org.jspecify.annotations.Nullable;

/**
 * The {@code requestId} and {@code traceId} on every problem (spec §13). {@code RequestCorrelationFilter} starts
 * one before any other filter sees the request, so it is always set by the time a controller or
 * {@code ApiExceptionHandler} runs. A real tracer arrives in session 2 step 6; until then {@code traceId} only
 * correlates a problem with the application log, not a trace backend.
 */
public final class RequestCorrelation {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ThreadLocal<RequestCorrelation> CURRENT = new ThreadLocal<>();

    private final String requestId;
    private final String traceId;

    private RequestCorrelation(String requestId, String traceId) {
        this.requestId = requestId;
        this.traceId = traceId;
    }

    public static RequestCorrelation start(@Nullable String clientRequestId) {
        String requestId = clientRequestId == null || clientRequestId.isBlank()
                ? "req_" + mintHex(16)
                : clientRequestId.strip();
        var correlation = new RequestCorrelation(requestId, mintHex(16));
        CURRENT.set(correlation);
        return correlation;
    }

    public static RequestCorrelation current() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public String requestId() {
        return requestId;
    }

    public String traceId() {
        return traceId;
    }

    private static String mintHex(int bytes) {
        byte[] data = new byte[bytes];
        RANDOM.nextBytes(data);
        return HexFormat.of().formatHex(data);
    }
}
