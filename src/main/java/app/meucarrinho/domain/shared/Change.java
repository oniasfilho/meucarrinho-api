package app.meucarrinho.domain.shared;

import java.util.Optional;

public sealed interface Change<T> {
    record Keep<T>() implements Change<T> {}

    record Set<T>(T value) implements Change<T> {}

    record Clear<T>() implements Change<T> {}

    static <T> Change<T> keep() {
        return new Keep<>();
    }

    static <T> Change<T> set(T value) {
        return new Set<>(value);
    }

    static <T> Change<T> clear() {
        return new Clear<>();
    }

    default boolean isKeep() {
        return this instanceof Keep;
    }

    default Optional<T> applyTo(Optional<T> current) {
        return switch (this) {
            case Keep<T> k -> current;
            case Set<T> s -> Optional.of(s.value());
            case Clear<T> c -> Optional.empty();
        };
    }
}
