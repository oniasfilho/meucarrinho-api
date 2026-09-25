package app.meucarrinho.application.lists;

import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.Budget;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.StoreName;
import java.util.Optional;

public record CreateListCommand(
        Optional<ListId> listId,
        AccountId owner,
        ListName name,
        Optional<StoreName> store,
        Optional<Budget> budget) {}
