package app.meucarrinho.testfixtures.telemetry;

import app.meucarrinho.application.telemetry.port.AttributeKey;
import app.meucarrinho.application.telemetry.port.Operation;
import app.meucarrinho.application.telemetry.port.Tracing;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class RecordingTracing implements Tracing {
    public record Span(Operation op, Map<AttributeKey, String> attributes, boolean failed) {}

    private final List<Span> spans = new ArrayList<>();

    @Override
    public <T> T inSpan(Operation op, Map<AttributeKey, String> attributes, Supplier<T> work) {
        try {
            T result = work.get();
            spans.add(new Span(op, Map.copyOf(attributes), false));
            return result;
        } catch (RuntimeException e) {
            spans.add(new Span(op, Map.copyOf(attributes), true));
            throw e;
        }
    }

    public List<Span> spans() {
        return List.copyOf(spans);
    }
}
