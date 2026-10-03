package app.meucarrinho.api.rest;

import app.meucarrinho.domain.shared.Change;
import java.util.function.Function;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * One field of a PATCH body: absent keeps the value, {@code null} clears it, anything else sets it. Jackson 3 maps
 * both absent and {@code null} to {@code Optional.empty()}, so the three cases need their own type. Declare the
 * component with {@code @JsonInclude(NON_EMPTY)} so a typed client omits fields it keeps.
 */
@JsonDeserialize(using = Patch.Reader.class)
@JsonSerialize(using = Patch.Writer.class)
public sealed interface Patch<T> {
    record Keep<T>() implements Patch<T> {}

    record Set<T>(T value) implements Patch<T> {}

    record Clear<T>() implements Patch<T> {}

    static <T> Patch<T> keep() {
        return new Keep<>();
    }

    static <T> Patch<T> set(T value) {
        return new Set<>(value);
    }

    static <T> Patch<T> clear() {
        return new Clear<>();
    }

    /** The domain change, converting a set value with {@code convert}. */
    default <U> Change<U> toChange(Function<T, U> convert) {
        return switch (this) {
            case Keep<T> k -> Change.keep();
            case Set<T> s -> Change.set(convert.apply(s.value()));
            case Clear<T> c -> Change.clear();
        };
    }

    final class Reader extends ValueDeserializer<Patch<?>> {
        private final ValueDeserializer<?> value;

        Reader() {
            this(null);
        }

        private Reader(ValueDeserializer<?> value) {
            this.value = value;
        }

        @Override
        public ValueDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) {
            JavaType type = property != null ? property.getType() : context.getContextualType();
            return new Reader(context.findContextualValueDeserializer(type.containedType(0), property));
        }

        @Override
        public Patch<?> deserialize(JsonParser parser, DeserializationContext context) {
            return new Set<>(value.deserialize(parser, context));
        }

        @Override
        public Patch<?> getNullValue(DeserializationContext context) {
            return new Clear<>();
        }

        @Override
        public Object getAbsentValue(DeserializationContext context) {
            return new Keep<>();
        }
    }

    final class Writer extends ValueSerializer<Patch<?>> {
        @Override
        public void serialize(Patch<?> patch, JsonGenerator generator, SerializationContext context) {
            if (patch instanceof Set<?>(Object value)) {
                context.writeValue(generator, value);
            } else {
                generator.writeNull();
            }
        }

        @Override
        public boolean isEmpty(SerializationContext context, Patch<?> patch) {
            return patch instanceof Keep;
        }
    }
}
