package app.meucarrinho.domain.list;

import app.meucarrinho.domain.shared.Budget;
import app.meucarrinho.domain.shared.Change;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.StoreName;
import java.util.Optional;

public record ListDetailsChange(Optional<ListName> name, Change<StoreName> store, Change<Budget> budget) {
    public static ListDetailsChange none() {
        return new ListDetailsChange(Optional.empty(), Change.keep(), Change.keep());
    }

    public ListDetailsChange withName(ListName value) {
        return new ListDetailsChange(Optional.of(value), store, budget);
    }

    public ListDetailsChange withStore(Change<StoreName> value) {
        return new ListDetailsChange(name, value, budget);
    }

    public ListDetailsChange withBudget(Change<Budget> value) {
        return new ListDetailsChange(name, store, value);
    }
}
