package app.meucarrinho.application.catalog.port;

import app.meucarrinho.domain.shared.ItemName;
import app.meucarrinho.domain.shared.Money;
import java.util.Optional;

public record CatalogEntry(ItemName name, Optional<Money> lastKnownPrice, Category category) {}
