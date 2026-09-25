package app.meucarrinho.testfixtures.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import app.meucarrinho.application.catalog.port.CatalogEntry;
import app.meucarrinho.application.catalog.port.CatalogSource;
import app.meucarrinho.application.catalog.port.Category;
import app.meucarrinho.domain.shared.ItemName;
import app.meucarrinho.domain.shared.Money;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class CatalogSourceContract {
    protected static final List<CatalogEntry> ENTRIES = List.of(
            entry("Café Pilão 500g", 18_90, "mercearia"),
            entry("Café", 17_50, "mercearia"),
            entry("Cafeteira", 0, "utilidades"),
            entry("Leite integral", 5_49, "laticinios"),
            entry("Maçã", 8_99, "hortifruti"));

    private CatalogSource source;

    protected abstract CatalogSource createSource(List<CatalogEntry> entries);

    @BeforeEach
    void setUp() {
        source = createSource(ENTRIES);
    }

    private static CatalogEntry entry(String name, long price, String category) {
        return new CatalogEntry(new ItemName(name), price == 0 ? Optional.empty() : Optional.of(Money.brl(price)),
                new Category(category));
    }

    private List<String> names(String prefix, int limit) {
        return source.suggest(prefix, limit).orElseThrow().stream().map(e -> e.name().value()).toList();
    }

    @Test
    void matches_prefixes_without_accents_or_case() {
        assertThat(names("cafe", 10)).containsExactly("Café", "Cafeteira", "Café Pilão 500g");
        assertThat(names("MACA", 10)).containsExactly("Maçã");
    }

    @Test
    void respects_the_limit() {
        assertThat(names("caf", 2)).containsExactly("Café", "Cafeteira");
    }

    @Test
    void returns_prices_and_categories() {
        CatalogEntry leite = source.suggest("leite", 1).orElseThrow().getFirst();

        assertThat(leite.lastKnownPrice()).contains(Money.brl(5_49));
        assertThat(leite.category()).isEqualTo(new Category("laticinios"));
    }

    @Test
    void returns_nothing_for_a_blank_or_unknown_prefix() {
        assertThat(names("  ", 3)).isEmpty();
        assertThat(names("xyz", 3)).isEmpty();
    }
}
