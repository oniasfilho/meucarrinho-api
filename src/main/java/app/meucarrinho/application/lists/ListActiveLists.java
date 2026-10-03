package app.meucarrinho.application.lists;

import app.meucarrinho.domain.shared.AccountId;
import java.util.List;

public interface ListActiveLists {
    List<ActiveListCard> activeLists(AccountId member);
}
