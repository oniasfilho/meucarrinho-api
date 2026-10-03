package app.meucarrinho.application.common.port;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** The parts of a response a replay sends again: status, the headers worth keeping, and the body bytes. */
public record StoredResponse(int status, Map<String, String> headers, byte[] body) {
    public StoredResponse {
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("status must be 100 to 599");
        }
        headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        body = body.clone();
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof StoredResponse that && status == that.status && headers.equals(that.headers)
                && Arrays.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * status + headers.hashCode()) + Arrays.hashCode(body);
    }

    @Override
    public String toString() {
        return "StoredResponse[status=" + status + ", headers=" + headers + ", body=" + body.length + " bytes]";
    }
}
