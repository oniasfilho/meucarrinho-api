package app.meucarrinho.testfixtures.catalog;

import app.meucarrinho.application.catalog.port.CatalogEntry;
import app.meucarrinho.application.catalog.port.CatalogError;
import app.meucarrinho.application.catalog.port.CatalogSource;
import app.meucarrinho.domain.shared.ItemName;
import app.meucarrinho.domain.shared.Result;
import java.util.Comparator;
import java.util.List;

public final class InMemoryCatalogSource implements CatalogSource {
    private final List<CatalogEntry> entries;

    public InMemoryCatalogSource(List<CatalogEntry> entries) {
        this.entries = List.copyOf(entries);
    }

    @Override
    public Result<List<CatalogEntry>, CatalogError> suggest(String prefix, int limit) {
        if (prefix.isBlank()) {
            return Result.ok(List.of());
        }
        String key = new ItemName(prefix).normalized();
        return Result.ok(entries.stream()
                .filter(entry -> entry.name().normalized().startsWith(key))
                .sorted(Comparator.comparingInt((CatalogEntry e) -> e.name().normalized().length())
                        .thenComparing(e -> e.name().normalized()))
                .limit(limit)
                .toList());
    }
}
