package app.meucarrinho.api.rest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * A failure the API reports with a stable {@code code} (spec §13). Controllers throw it and
 * {@link ApiExceptionHandler} renders it; step 4 adds {@code requestId}, {@code traceId} and the full field-error
 * contract around it. It carries no stack trace: it is an answer, not a bug.
 */
public final class ApiProblem extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final HttpStatus status;
    private final String code;
    private final transient Map<String, Object> properties;

    public ApiProblem(HttpStatus status, String code, String message) {
        this(status, code, message, Map.of());
    }

    public ApiProblem(HttpStatus status, String code, String message, Map<String, Object> properties) {
        super(message, null, false, false);
        this.status = status;
        this.code = code;
        this.properties = Map.copyOf(new LinkedHashMap<>(properties));
    }

    public static ApiProblem malformed(String message) {
        return new ApiProblem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", message);
    }

    public static ApiProblem unauthenticated() {
        return new ApiProblem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Sign in to use this endpoint.");
    }

    public static ApiProblem preconditionRequired() {
        return new ApiProblem(HttpStatus.PRECONDITION_REQUIRED, "PRECONDITION_REQUIRED",
                "List writes need If-Match with the version you last read.");
    }

    public static ApiProblem invalid(String field, String code, String message) {
        return new ApiProblem(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED", "The request has invalid fields.",
                Map.of("errors", List.of(Map.of("field", field, "code", code, "message", message))));
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public Map<String, Object> properties() {
        return properties;
    }
}
