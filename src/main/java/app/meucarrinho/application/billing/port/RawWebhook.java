package app.meucarrinho.application.billing.port;

import java.util.List;
import java.util.Map;

public record RawWebhook(String provider, Map<String, List<String>> headers, byte[] body) {
    public RawWebhook {
        headers = Map.copyOf(headers);
        body = body.clone();
    }

    @Override
    public byte[] body() {
        return body.clone();
    }
}
