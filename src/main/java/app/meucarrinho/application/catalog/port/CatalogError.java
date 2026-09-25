package app.meucarrinho.application.catalog.port;

public sealed interface CatalogError {
    record SourceUnavailable(String reason) implements CatalogError {}
}
