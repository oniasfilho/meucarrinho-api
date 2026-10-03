package app.meucarrinho.api.rest;

import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * List versions on the wire: responses send {@code ETag: "<version>"} and list writes must send it back in
 * {@code If-Match}, quoted or bare. The use case decides whether it is stale (ADR 0006); this only parses.
 */
public final class Revisions {
    private static final Pattern VERSION = Pattern.compile("\"?(\\d{1,18})\"?");

    private Revisions() {}

    public static long expected(@Nullable String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw ApiProblem.preconditionRequired();
        }
        var matcher = VERSION.matcher(ifMatch.strip());
        if (!matcher.matches()) {
            throw ApiProblem.malformed("If-Match must be the list version, for example \"12\".");
        }
        return Long.parseLong(matcher.group(1));
    }

    public static String etag(long version) {
        return "\"" + version + "\"";
    }
}
