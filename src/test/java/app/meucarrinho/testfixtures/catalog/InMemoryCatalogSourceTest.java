package app.meucarrinho.testfixtures.catalog;

import app.meucarrinho.application.catalog.port.CatalogEntry;
import app.meucarrinho.application.catalog.port.CatalogSource;
import java.util.List;

class InMemoryCatalogSourceTest extends CatalogSourceContract {
    @Override
    protected CatalogSource createSource(List<CatalogEntry> entries) {
        return new InMemoryCatalogSource(entries);
    }
}
