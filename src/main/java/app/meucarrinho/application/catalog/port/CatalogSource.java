package app.meucarrinho.application.catalog.port;

import app.meucarrinho.domain.shared.Result;
import java.util.List;

public interface CatalogSource {
    Result<List<CatalogEntry>, CatalogError> suggest(String prefix, int limit);
}
