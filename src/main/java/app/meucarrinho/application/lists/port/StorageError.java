package app.meucarrinho.application.lists.port;

public sealed interface StorageError {
    record TooLarge(long maxBytes) implements StorageError {}

    record UnsupportedType(String mediaType) implements StorageError {}

    record NotFound() implements StorageError {}

    record ProviderUnavailable(String reason) implements StorageError {}
}
