package app.meucarrinho.api.rest;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Builds a domain value from request input and names the field when the value object refuses it. The message never
 * echoes the input back (spec §13: no request content in error bodies).
 */
public final class Fields {
    private Fields() {}

    public static <T> T parse(String field, Supplier<T> conversion) {
        try {
            return conversion.get();
        } catch (IllegalArgumentException e) {
            throw ApiProblem.invalid(field, "INVALID_VALUE", "This value is not allowed here.");
        }
    }

    public static <T> T required(String field, @Nullable T value) {
        if (value == null) {
            throw ApiProblem.invalid(field, "REQUIRED", "This field is required.");
        }
        return value;
    }

    /** Path and body IDs are UUIDv7 (spec §4); anything else cannot name a resource. */
    public static <T> T id(String what, Supplier<T> conversion) {
        try {
            return conversion.get();
        } catch (IllegalArgumentException e) {
            throw ApiProblem.malformed(what + " must be a UUIDv7.");
        }
    }
}
