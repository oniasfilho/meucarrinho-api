package app.meucarrinho.domain.shared;

import java.util.function.Function;
import org.jspecify.annotations.Nullable;

public sealed interface Result<T extends @Nullable Object, E> {
    record Ok<T extends @Nullable Object, E>(T value) implements Result<T, E> {}

    record Err<T extends @Nullable Object, E>(E error) implements Result<T, E> {}

    static <T extends @Nullable Object, E> Result<T, E> ok(T value) {
        return new Ok<>(value);
    }

    static <E> Result<@Nullable Void, E> ok() {
        return new Ok<>(null);
    }

    static <T extends @Nullable Object, E> Result<T, E> err(E error) {
        return new Err<>(error);
    }

    default boolean isOk() {
        return this instanceof Ok;
    }

    default T orElseThrow() {
        return switch (this) {
            case Ok<T, E> ok -> ok.value();
            case Err<T, E> err -> throw new IllegalStateException("Expected Ok but was " + err.error());
        };
    }

    default E errorOrThrow() {
        return switch (this) {
            case Ok<T, E> ok -> throw new IllegalStateException("Expected Err but was Ok(" + ok.value() + ")");
            case Err<T, E> err -> err.error();
        };
    }

    default <U extends @Nullable Object> Result<U, E> map(Function<? super T, ? extends U> mapper) {
        return switch (this) {
            case Ok<T, E> ok -> new Ok<>(mapper.apply(ok.value()));
            case Err<T, E> err -> new Err<>(err.error());
        };
    }

    default <U extends @Nullable Object> Result<U, E> flatMap(Function<? super T, Result<U, E>> mapper) {
        return switch (this) {
            case Ok<T, E> ok -> mapper.apply(ok.value());
            case Err<T, E> err -> new Err<>(err.error());
        };
    }
}
