package app.meucarrinho.api.rest;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Turns every failure into Problem Details with a stable {@code code}, a {@code requestId} and a {@code traceId}
 * (spec §13). Nothing unmapped ever reaches the body: it becomes a bare {@code 500 INTERNAL}, with the exception
 * logged with its stack trace instead.
 */
@RestControllerAdvice
class ApiExceptionHandler {
    private static final String PROBLEMS = "https://api.meucarrinho.app/problems/";
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiProblem.class)
    ResponseEntity<ProblemDetail> problem(ApiProblem problem) {
        return render(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> invalid(MethodArgumentNotValidException e) {
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "code", "INVALID_VALUE",
                        "message", "This field is missing or has an invalid value."))
                .toList();
        return render(new ApiProblem(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED",
                "The request has invalid fields.", Map.of("errors", errors)));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ProblemDetail> malformed(Exception e) {
        return render(ApiProblem.malformed("The request could not be read."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> internal(Exception e) {
        LOG.error("Unhandled exception", e);
        return render(ApiProblem.internal());
    }

    private static ResponseEntity<ProblemDetail> render(ApiProblem problem) {
        ProblemDetail detail = ProblemDetail.forStatus(problem.status());
        detail.setType(URI.create(PROBLEMS + problem.code().toLowerCase(Locale.ROOT).replace('_', '-')));
        detail.setTitle(title(problem.code()));
        detail.setProperty("code", problem.code());
        detail.setProperty("message", problem.getMessage());
        RequestCorrelation correlation = RequestCorrelation.current();
        detail.setProperty("requestId", correlation.requestId());
        detail.setProperty("traceId", correlation.traceId());
        problem.properties().forEach(detail::setProperty);
        return ResponseEntity.status(problem.status()).body(detail);
    }

    private static String title(String code) {
        String words = code.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
