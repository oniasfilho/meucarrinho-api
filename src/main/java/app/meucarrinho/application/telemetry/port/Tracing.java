package app.meucarrinho.application.telemetry.port;

import java.util.Map;
import java.util.function.Supplier;

public interface Tracing {
    <T> T inSpan(Operation op, Map<AttributeKey, String> attributes, Supplier<T> work);
}
